/** 
 * Copyright (c) 2009, Regents of the University of Colorado 
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

package org.cleartk.examples.pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.pear.util.FileUtil;
import org.cleartk.classifier.CleartkAnnotatorDescriptionFactory;
import org.cleartk.classifier.CleartkSequenceAnnotator;
import org.cleartk.classifier.jar.DefaultDataWriterFactory;
import org.cleartk.classifier.jar.DefaultSequenceDataWriterFactory;
import org.cleartk.classifier.jar.DirectoryDataWriterFactory;
import org.cleartk.classifier.libsvm.MultiClassLIBSVMDataWriter;
import org.cleartk.classifier.mallet.DefaultMalletCRFDataWriterFactory;
import org.cleartk.classifier.mallet.DefaultMalletDataWriterFactory;
import org.cleartk.classifier.mallet.MalletCRFDataWriter;
import org.cleartk.classifier.mallet.MalletDataWriter;
import org.cleartk.classifier.mallet.MalletDataWriterFactory_ImplBase;
import org.cleartk.classifier.opennlp.DefaultMaxentDataWriterFactory;
import org.cleartk.classifier.opennlp.MaxentDataWriter;
import org.cleartk.classifier.opennlp.MaxentDataWriterFactory_ImplBase;
import org.cleartk.classifier.svmlight.OVASVMlightDataWriter;
import org.cleartk.classifier.svmlight.RunSVMlightTest;
import org.cleartk.classifier.viterbi.DefaultOutcomeFeatureExtractor;
import org.cleartk.classifier.viterbi.ViterbiClassifier;
import org.cleartk.classifier.viterbi.ViterbiDataWriterFactory;
import org.cleartk.examples.ExampleComponents;
import org.cleartk.examples.ExamplesTestBase;
import org.cleartk.syntax.constituent.TreebankConstants;
import org.cleartk.syntax.constituent.TreebankGoldAnnotator;
import org.cleartk.syntax.opennlp.SentenceAnnotator;
import org.cleartk.token.stem.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.util.cr.FilesCollectionReader;
import org.junit.Test;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.ResourceCreationSpecifierFactory;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.testing.util.HideOutput;

/**
 * <br>
 * Copyright (c) 2009, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Philip Ogren
 */

public class ExamplePosClassifierTest extends ExamplesTestBase {

  @Test
  public void testLibsvm() throws Exception {
    String outDirectoryName = outputDirectoryName + "/libsvm";

    AnalysisEngineDescription dataWriter = AnalysisEngineFactory.createPrimitiveDescription(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        CleartkSequenceAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
        ViterbiDataWriterFactory.class.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outDirectoryName,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        MultiClassLIBSVMDataWriter.class.getName(),
        ViterbiDataWriterFactory.PARAM_OUTCOME_FEATURE_EXTRACTOR_NAMES,
        new String[] { DefaultOutcomeFeatureExtractor.class.getName() });

    testClassifier(dataWriter, outDirectoryName, 1, "-t", "0"); // MultiClassLIBSVMClassifier.score
                                                                // is not implemented so we cannot
                                                                // have a stack size greater than 1.
    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0].trim();
    boolean badTags = firstLine.equals("2008/NN Sichuan/NN earthquake/NN From/NN Wikipedia/NN ,/NN the/NN free/NN encyclopedia/NN");
    assertFalse(badTags);
    assertEquals(
        "2008/NN Sichuan/NN earthquake/NN From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/NN",
        firstLine);

  }

  @Test
  public void testMalletCRF() throws Exception {
    this.assumeLongTestsEnabled();
    this.logger.info(LONG_TEST_MESSAGE);

    String outDirectoryName = outputDirectoryName + "/malletcrf";
    AnalysisEngineDescription dataWriter = AnalysisEngineFactory.createPrimitiveDescription(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        DefaultSequenceDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        MalletCRFDataWriter.class.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outDirectoryName);
    testClassifier(dataWriter, outDirectoryName, -1); // viterbi stack size is meaningless here so
                                                      // pass in an invalid value to make sure it is
                                                      // ignored.

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0].trim();
    assertEquals(
        "2008/NN Sichuan/CD earthquake/NNS From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/NN",
        firstLine);

  }

  @Test
  @Deprecated
  public void testMalletCRF2() throws Exception {
    this.assumeLongTestsEnabled();
    this.logger.info(LONG_TEST_MESSAGE);

    String outDirectoryName = outputDirectoryName + "/malletcrf-compressed";
    AnalysisEngineDescription dataWriter = CleartkAnnotatorDescriptionFactory.createCleartkSequenceAnnotator(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        DefaultMalletCRFDataWriterFactory.class,
        outDirectoryName);
    ResourceCreationSpecifierFactory.setConfigurationParameters(
        dataWriter,
        DefaultMalletCRFDataWriterFactory.PARAM_COMPRESS,
        true);
    testClassifier(dataWriter, outDirectoryName, -1); // viterbi stack size is meaningless here so
                                                      // pass in an invalid value to make sure it is
                                                      // ignored.

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0].trim();
    assertEquals(
        "2008/DT Sichuan/JJ earthquake/NNS From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/NN",
        firstLine);
  }

  @Test
  public void testMaxent() throws Exception {
    String outDirectoryName = outputDirectoryName + "/maxent";

    AnalysisEngineDescription dataWriter = AnalysisEngineFactory.createPrimitiveDescription(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        CleartkSequenceAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
        ViterbiDataWriterFactory.class.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outDirectoryName,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        MaxentDataWriter.class.getName(),
        ViterbiDataWriterFactory.PARAM_OUTCOME_FEATURE_EXTRACTOR_NAMES,
        new String[] { DefaultOutcomeFeatureExtractor.class.getName() });
    testClassifier(dataWriter, outDirectoryName, 10);

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0];
    assertEquals(
        "2008/CD Sichuan/JJ earthquake/NNS From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/IN",
        firstLine);
  }

  @Test
  @Deprecated
  public void testMaxent2() throws Exception {
    String outDirectoryName = outputDirectoryName + "/maxent2";

    AnalysisEngineDescription dataWriter = CleartkAnnotatorDescriptionFactory.createViterbiAnnotator(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        DefaultMaxentDataWriterFactory.class,
        outDirectoryName);
    ConfigurationParameterFactory.addConfigurationParameter(
        dataWriter,
        MaxentDataWriterFactory_ImplBase.PARAM_COMPRESS,
        true);
    testClassifier(dataWriter, outDirectoryName, 10);

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0];
    assertEquals(
        "2008/CD Sichuan/JJ earthquake/NNS From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/IN",
        firstLine);
  }

  @Test
  public void testMalletMaxent() throws Exception {
    String outDirectoryName = outputDirectoryName + "/mallet-maxent";

    AnalysisEngineDescription dataWriter = AnalysisEngineFactory.createPrimitiveDescription(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        CleartkSequenceAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
        ViterbiDataWriterFactory.class.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outDirectoryName,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        MalletDataWriter.class.getName(),
        ViterbiDataWriterFactory.PARAM_OUTCOME_FEATURE_EXTRACTOR_NAMES,
        new String[] { DefaultOutcomeFeatureExtractor.class.getName() });
    testClassifier(dataWriter, outDirectoryName, 10, "MaxEnt");

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0];
    assertEquals(
        "2008/DT Sichuan/JJ earthquake/NN From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/NN",
        firstLine);
  }

  @Test
  public void testMalletNaiveBayes() throws Exception {
    String outDirectoryName = outputDirectoryName + "/mallet-naive-bayes";

    AnalysisEngineDescription dataWriter = AnalysisEngineFactory.createPrimitiveDescription(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        CleartkSequenceAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
        ViterbiDataWriterFactory.class.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outDirectoryName,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        MalletDataWriter.class.getName(),
        ViterbiDataWriterFactory.PARAM_OUTCOME_FEATURE_EXTRACTOR_NAMES,
        new String[] { DefaultOutcomeFeatureExtractor.class.getName() });
    testClassifier(dataWriter, outDirectoryName, 10, "NaiveBayes");

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0];
    assertEquals(
        "2008/DT Sichuan/JJ earthquake/NN From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/IN",
        firstLine);
  }

  @Test
  @Deprecated
  public void testMalletNaiveBayes2() throws Exception {
    String outDirectoryName = outputDirectoryName + "/mallet-naive-bayes";

    AnalysisEngineDescription dataWriter = CleartkAnnotatorDescriptionFactory.createViterbiAnnotator(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        DefaultMalletDataWriterFactory.class,
        outDirectoryName);
    ConfigurationParameterFactory.addConfigurationParameter(
        dataWriter,
        MalletDataWriterFactory_ImplBase.PARAM_COMPRESS,
        true);
    testClassifier(dataWriter, outDirectoryName, 10, "NaiveBayes");

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0];
    assertEquals(
        "2008/DT Sichuan/JJ earthquake/NN From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/IN",
        firstLine);
  }

  @Test
  public void testMalletC45() throws Exception {
    this.assumeLongTestsEnabled();
    this.logger.info(LONG_TEST_MESSAGE);

    String outDirectoryName = outputDirectoryName + "/mallet-c45";

    AnalysisEngineDescription dataWriter = AnalysisEngineFactory.createPrimitiveDescription(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        CleartkSequenceAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
        ViterbiDataWriterFactory.class.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outDirectoryName,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        MalletDataWriter.class.getName(),
        ViterbiDataWriterFactory.PARAM_OUTCOME_FEATURE_EXTRACTOR_NAMES,
        new String[] { DefaultOutcomeFeatureExtractor.class.getName() });
    testClassifier(dataWriter, outDirectoryName, 10, "C45");

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0];
    assertEquals(
        "2008/CD Sichuan/JJ earthquake/NN From/NN Wikipedia/NN ,/, the/DT free/NN encyclopedia/NN",
        firstLine);
  }

  @Test
  public void testSVMLIGHT() throws Exception {
    this.assumeTestsEnabled(RunSVMlightTest.SVMLIGHT_TESTS_PROPERTY_VALUE);
    this.logger.info(RunSVMlightTest.SVMLIGHT_TESTS_ENABLED_MESSAGE);

    String outDirectoryName = outputDirectoryName + "/svmlight";
    AnalysisEngineDescription dataWriter = AnalysisEngineFactory.createPrimitiveDescription(
        ExamplePOSAnnotator.class,
        ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
        CleartkSequenceAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
        ViterbiDataWriterFactory.class.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outDirectoryName,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        OVASVMlightDataWriter.class.getName(),
        ViterbiDataWriterFactory.PARAM_OUTCOME_FEATURE_EXTRACTOR_NAMES,
        new String[] { DefaultOutcomeFeatureExtractor.class.getName() });
    testClassifier(dataWriter, outDirectoryName, 1);

    String firstLine = FileUtil.loadListOfStrings(new File(outDirectoryName
        + "/2008_Sichuan_earthquake.txt.pos"))[0].trim();
    boolean badTags = firstLine.equals("2008/NN Sichuan/NN earthquake/NN From/NN Wikipedia/NN ,/NN the/NN free/NN encyclopedia/NN");
    assertFalse(badTags);

    assertEquals(
        "2008/CD Sichuan/JJ earthquake/NNS From/IN Wikipedia/NN ,/, the/DT free/NN encyclopedia/IN",
        firstLine);
  }

  private void testClassifier(
      AnalysisEngineDescription dataWriter,
      String outDirectoryName,
      int stackSize,
      String... trainingArgs) throws Exception {

    SimplePipeline.runPipeline(
        FilesCollectionReader.getCollectionReaderWithView(
            "src/test/resources/data/treebank/11597317.tree",
            TreebankConstants.TREEBANK_VIEW),
        TreebankGoldAnnotator.getDescriptionPOSTagsOnly(),
        DefaultSnowballStemmer.getDescription("English"),
        dataWriter);

    String[] args;
    if (trainingArgs != null && trainingArgs.length > 0) {
      args = new String[trainingArgs.length + 1];
      args[0] = outDirectoryName;
      System.arraycopy(trainingArgs, 0, args, 1, trainingArgs.length);
    } else {
      args = new String[] { outDirectoryName };
    }

    HideOutput hider = new HideOutput();
    try {
      org.cleartk.classifier.jar.Train.main(args);
    } finally {
      hider.restoreOutput();
    }

    AnalysisEngineDescription taggerDescription = ExamplePOSAnnotator.getClassifierDescription(outDirectoryName
        + "/model.jar");
    ResourceCreationSpecifierFactory.setConfigurationParameters(
        taggerDescription,
        ViterbiClassifier.PARAM_STACK_SIZE,
        stackSize);

    SimplePipeline.runPipeline(
        FilesCollectionReader.getCollectionReader("src/test/resources/data/2008_Sichuan_earthquake.txt"),
        SentenceAnnotator.getDescription(),
        TokenAnnotator.getDescription(),
        DefaultSnowballStemmer.getDescription("English"),
        taggerDescription,
        AnalysisEngineFactory.createPrimitiveDescription(
            ExamplePOSPlainTextWriter.class,
            ExampleComponents.TYPE_SYSTEM_DESCRIPTION,
            ExamplePOSPlainTextWriter.PARAM_OUTPUT_DIRECTORY_NAME,
            outDirectoryName));
  }

}
