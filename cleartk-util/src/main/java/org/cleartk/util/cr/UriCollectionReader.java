/** 
 * Copyright (c) 2012, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package org.cleartk.util.cr;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.cleartk.util.ViewURIUtil;
import org.uimafit.component.JCasCollectionReader_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.ConfigurationParameterFactory;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * <p>
 * 
 * A CollectionReader that populates the default sofa with URI. This can accept a Collection of
 * Files, Collection of URIs or a single directory. If given a directory it will create a jCas for
 * each file within the directory (recursive).
 * <p>
 * This should be used in conjunction with UriToDocumentTextAnnotator.
 * 
 * @author Lee Becker
 * 
 */
public class UriCollectionReader extends JCasCollectionReader_ImplBase {

  public static CollectionReaderDescription getDescriptionFromDirectory(String directory)
      throws ResourceInitializationException {
    return CollectionReaderFactory.createDescription(
        UriCollectionReader.class,
        null,
        PARAM_DIRECTORY,
        directory);
  }

  public static CollectionReader getCollectionReaderFromDirectory(String directory)
      throws ResourceInitializationException {
    return CollectionReaderFactory.createCollectionReader(getDescriptionFromDirectory(directory));
  }

  public static CollectionReaderDescription getDescriptionFromFiles(List<File> files)
      throws ResourceInitializationException {

    String[] paths = new String[files.size()];
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = files.get(i).getPath();
    }
    return CollectionReaderFactory.createDescription(
        UriCollectionReader.class,
        null,
        PARAM_FILES,
        paths);
  }

  public static CollectionReader getCollectionReaderFromFiles(List<File> files)
      throws ResourceInitializationException {
    return CollectionReaderFactory.createCollectionReader(getDescriptionFromFiles(files));
  }

  public static CollectionReaderDescription getDescriptionFromUris(List<URI> uris)
      throws ResourceInitializationException {

    String[] uriStrings = new String[uris.size()];
    for (int i = 0; i < uriStrings.length; ++i) {
      uriStrings[i] = uris.get(i).toString();
    }
    return CollectionReaderFactory.createDescription(
        UriCollectionReader.class,
        null,
        PARAM_FILES,
        uriStrings);
  }

  public static CollectionReader getCollectionReaderFromUris(List<URI> uris)
      throws ResourceInitializationException {
    return CollectionReaderFactory.createCollectionReader(getDescriptionFromUris(uris));
  }

  public static final String PARAM_FILES = ConfigurationParameterFactory.createConfigurationParameterName(
      UriCollectionReader.class,
      "fileList");

  @ConfigurationParameter(
      description = "provides a list of files whose URI should be written to the default sofa within the CAS")
  private List<File> fileList = new ArrayList<File>();

  public static final String PARAM_DIRECTORY = ConfigurationParameterFactory.createConfigurationParameterName(
      UriCollectionReader.class,
      "directory");

  @ConfigurationParameter(
      description = "provids a directory containing files whose URIs should be written to the defaul sofa within the CAS")
  private File directory = null;

  public static final String PARAM_URIS = ConfigurationParameterFactory.createConfigurationParameterName(
      UriCollectionReader.class,
      "uriList");

  @ConfigurationParameter(
      description = "provides a list of URIs that should be written to the default sofa within the CAS")
  private List<String> uriList = new ArrayList<String>();

  protected Iterable<URI> uris;

  protected Iterator<URI> uriIter;

  protected int numUrisCompleted = 0;

  protected int uriCount = 0;

  protected Function<String, URI> stringToUri = new Function<String, URI>() {
    @Override
    public URI apply(String input) {
      try {
        return new URI(input);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  };

  protected Function<File, URI> fileToUri = new Function<File, URI>() {
    @Override
    public URI apply(File input) {
      return input.toURI();
    }
  };

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {

    // Convert list of strings to URIs
    Iterable<URI> urisFromUris = new ArrayList<URI>();
    this.uriCount += this.uriList.size();
    urisFromUris = Iterables.transform(this.uriList, this.stringToUri);

    // Convert list of files to URIs
    Iterable<URI> urisFromFiles = new ArrayList<URI>();
    this.uriCount += this.fileList.size();
    urisFromFiles = Iterables.transform(this.fileList, this.fileToUri);

    // Read file names from directory and convert list of files to URI
    Iterable<URI> urisFromDirectory = new ArrayList<URI>();
    if (this.isRootDirectoryValid()) {
      IOFileFilter svnFileFilter = FileFilterUtils.makeSVNAware(null);
      IOFileFilter dirFilter = FileFilterUtils.makeSVNAware(FileFilterUtils.directoryFileFilter());
      Collection<File> files = FileUtils.listFiles(this.directory, svnFileFilter, dirFilter);
      this.uriCount += files.size();
      urisFromDirectory = Iterables.transform(files, this.fileToUri);
    }

    this.uris = Iterables.concat(urisFromUris, urisFromFiles, urisFromDirectory);
    this.uriIter = this.uris.iterator();
  }

  private boolean isRootDirectoryValid() throws ResourceInitializationException {
    if (this.directory == null) {
      return false;
    }

    if (!this.directory.exists()) {
      String format = "Directory %s does not exist";
      String message = String.format(format, directory.getPath());
      throw new ResourceInitializationException(new IOException(message));
    }

    if (!this.directory.isDirectory()) {
      String format = "Directory %s is not a directory.  For specifi files set PARAM_FILES instead of PARAM_DIRECTORY.";
      String message = String.format(format, directory.getPath());
      throw new ResourceInitializationException(new IOException(message));
    }
    return true;
  }

  @Override
  public boolean hasNext() throws IOException, CollectionException {
    return this.uriIter.hasNext();
  }

  @Override
  public Progress[] getProgress() {
    Progress progress = new ProgressImpl(numUrisCompleted, uriCount, Progress.ENTITIES);
    return new Progress[] { progress };
  }

  @Override
  public void getNext(JCas jCas) throws IOException, CollectionException {
    if (!this.hasNext()) {
      throw new RuntimeException("getNext(jCas) was called but hasNext() returns false");
    }

    ViewURIUtil.setURI(jCas, this.uriIter.next());
  }

}
