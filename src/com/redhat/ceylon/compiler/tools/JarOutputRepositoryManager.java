/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.redhat.ceylon.compiler.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.tools.JavaFileObject;

import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.util.ShaSigner;
import com.redhat.ceylon.compiler.util.Util;
import com.sun.tools.javac.main.OptionName;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;

public class JarOutputRepositoryManager {
    
    private Map<Module,ProgressiveJar> openJars = new HashMap<Module, ProgressiveJar>();
    private Log log;
    private Options options;
    private CeyloncFileManager ceyloncFileManager;
    
    JarOutputRepositoryManager(Log log, Options options, CeyloncFileManager ceyloncFileManager){
        this.log = log;
        this.options = options;
        this.ceyloncFileManager = ceyloncFileManager;
    }
    
    public JavaFileObject getFileObject(File outputDir, Module module, String fileName, File sourceFile) throws IOException{
        ProgressiveJar progressiveJar = getProgressiveJar(outputDir, module);
        progressiveJar.addSource(sourceFile);
        return progressiveJar.getJavaFileObject(fileName);
    }
    
    private ProgressiveJar getProgressiveJar(File outputDir, Module module) throws IOException {
        ProgressiveJar jarFile = openJars.get(module);
        if(jarFile == null){
            jarFile = new ProgressiveJar(outputDir, module, log, options, ceyloncFileManager);
            openJars.put(module, jarFile);
        }
        return jarFile;
    }

    public void flush(){
        for(ProgressiveJar jarFile : openJars.values()){
            flush(jarFile);
        }
        openJars.clear();
    }
    
    private void flush(ProgressiveJar jarFile) {
        try {
            jarFile.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    static class ProgressiveJar {
        private File originalJarFile;
        private File outputJarFile;
        private JarOutputStream jarOutputStream;
        private File originalSrcFile;
        private File outputSrcFile;
        private JarOutputStream srcOutputStream;
        final private Set<String> writtenClasses = new HashSet<String>();
        final private Set<String> sourceFiles = new HashSet<String>();
        private Log log;
        private Options options;
        private CeyloncFileManager ceyloncFileManager;
        
        public ProgressiveJar(File outputDir, Module module, Log log, Options options, CeyloncFileManager ceyloncFileManager) throws IOException{
            this.log = log;
            this.options = options;
            this.ceyloncFileManager = ceyloncFileManager;

            // figure out where it all goes
            String jarName = Util.getModuleArchiveName(module);
            String srcName = Util.getSourceArchiveName(module);
            File moduleOutputDir = Util.getModulePath(outputDir, module);
            // make sure the folder exists
            if(!moduleOutputDir.exists() && !moduleOutputDir.mkdirs())
                throw new IOException("Failed to create output dir: "+moduleOutputDir);
            if(options.get(OptionName.VERBOSE) != null){
                Log.printLines(log.noticeWriter, "[output jar name: "+jarName+"]");
            }
            setupJarOutput(moduleOutputDir, jarName);
            setupSrcOutput(moduleOutputDir, srcName);
        }

        private void setupJarOutput(File moduleOutputDir, String jarName) throws IOException {
            // now see if we create a new file or update one
            File targetJarFile = new File(moduleOutputDir, jarName);
            if(targetJarFile.exists()){
                outputJarFile = File.createTempFile(targetJarFile.getName(), ".tmp", targetJarFile.getParentFile());
                originalJarFile = targetJarFile;
            }else{
                outputJarFile = targetJarFile; 
                originalJarFile = null;
            }
            jarOutputStream = new JarOutputStream(new FileOutputStream(outputJarFile));
        }

        private void setupSrcOutput(File moduleOutputDir, String srcName) throws IOException {
            // now see if we create a new file or update one
            File targetSrcFile = new File(moduleOutputDir, srcName);
            if(targetSrcFile.exists()){
                outputSrcFile = File.createTempFile(targetSrcFile.getName(), ".tmp", targetSrcFile.getParentFile());
                originalSrcFile = targetSrcFile;
            }else{
                outputSrcFile = targetSrcFile; 
                originalSrcFile = null;
            }
            srcOutputStream = new JarOutputStream(new FileOutputStream(outputSrcFile));
        }

        public void addSource(File sourceFile) {
            sourceFiles.add(sourceFile.getPath());
        }

        public void close() throws IOException {
            finishUpdatingJar(originalJarFile, outputJarFile, jarOutputStream, writtenClasses);
            Set<String> copiedSourceFiles = copySourceFiles();
            finishUpdatingJar(originalSrcFile, outputSrcFile, srcOutputStream, copiedSourceFiles);
        }
        
        private Set<String> copySourceFiles() throws IOException {
            Set<String> copiedFiles = new HashSet<String>();
            for(String prefixedSourceFile : sourceFiles){
                // must remove the prefix first
                String sourceFile = Util.getSourceFilePath(ceyloncFileManager, prefixedSourceFile);
                // zips are UNIX-friendly
                sourceFile = sourceFile.replace(File.separatorChar, '/');
                srcOutputStream.putNextEntry(new ZipEntry(sourceFile));
                InputStream inputStream = new FileInputStream(prefixedSourceFile);
                copy(inputStream, srcOutputStream);
                inputStream.close();
                srcOutputStream.closeEntry();
                copiedFiles.add(sourceFile);
            }
            return copiedFiles;
        }

        public void finishUpdatingJar(File originalFile, File outputFile, 
                JarOutputStream jarOutputStream, Set<String> skipEntries) throws IOException {
            // now copy all previous jar entries
            if(originalFile != null){
                JarFile jarFile = new JarFile(originalFile);
                Enumeration<JarEntry> entries = jarFile.entries();
                while(entries.hasMoreElements()){
                    JarEntry entry = entries.nextElement();
                    // skip the old entry if we overwrote it
                    if(skipEntries.contains(entry.getName()))
                        continue;
                    jarOutputStream.putNextEntry(entry);
                    InputStream inputStream = jarFile.getInputStream(entry);
                    copy(inputStream, jarOutputStream);
                    inputStream.close();
                    jarOutputStream.closeEntry();
                }
                jarFile.close();
            }
            jarOutputStream.flush();
            jarOutputStream.close();
            if(options.get(OptionName.VERBOSE) != null){
                Log.printLines(log.noticeWriter, "[done writing to jar: "+outputFile.getPath()+"]");
            }
            if(originalFile != null){
                // now rename to the original name
                if(options.get(OptionName.VERBOSE) != null){
                    Log.printLines(log.noticeWriter, "[renaming jar to: "+originalFile.getPath()+"]");
                }
            	originalFile.delete();
                if(!outputFile.renameTo(originalFile))
                    throw new IOException("Failed to rename jar file");
                ShaSigner.sign(originalFile, log, options);
            }else
                ShaSigner.sign(outputFile, log, options);
        }

        private void copy(InputStream inputStream, JarOutputStream outputStream) throws IOException {
            byte[] buffer = new byte[4096];
            int read;
            while((read = inputStream.read(buffer)) != -1){
                outputStream.write(buffer, 0, read);
            }
        }

        public JavaFileObject getJavaFileObject(String fileName) {
            // record the class file we produce so that we don't save it from the original jar
        	fileName = fileName.replace(File.separatorChar, '/');
            writtenClasses.add(fileName);
            return new JarEntryFileObject(outputJarFile.getPath(), jarOutputStream, fileName);
        }
    }
}
