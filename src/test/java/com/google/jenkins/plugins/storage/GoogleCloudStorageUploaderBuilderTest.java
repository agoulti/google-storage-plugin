/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.storage;

import static com.google.jenkins.plugins.storage.AbstractUploadDescriptor.GCS_SCHEME;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.storage.GoogleCloudStorageUploaderBuilder.DescriptorImpl;
import com.google.jenkins.plugins.util.ConflictException;
import com.google.jenkins.plugins.util.ForbiddenException;
import com.google.jenkins.plugins.util.MockExecutor;
import com.google.jenkins.plugins.util.NotFoundException;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.Shell;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link GoogleCloudStorageUploaderBuilder}.
 */
public class GoogleCloudStorageUploaderBuilderTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Mock
  private GoogleRobotCredentials credentials;

  private GoogleCredential credential;

  private String bucket;
  private String glob;

  private FreeStyleProject project;
  private GoogleCloudStorageUploaderBuilder underTest;
  private boolean sharedPublicly;
  private boolean forFailedJobs;
  private boolean showInline;
  private boolean stripPathPrefix;
  private String pathPrefix;

  private final MockExecutor executor = new MockExecutor();
  private ConflictException conflictException;
  private ForbiddenException forbiddenException;
  private NotFoundException notFoundException;

  private static class MockUploadModule extends UploadModule {
    public MockUploadModule(MockExecutor executor) {
      this.executor = executor;
    }

    @Override
    public MockExecutor newExecutor() {
      return executor;
    }
    private final MockExecutor executor;
  }

  @Rule
  public Verifier verifySawAll = new Verifier() {
      @Override
      public void verify() {
        assertTrue(executor.sawAll());
        assertFalse(executor.sawUnexpected());
      }
    };

  /**
   * Checks that any object insertion that we do has certain
   * properties at the point of execution.
   */
  private Predicate<Storage.Objects.Insert> checkFieldsMatch =
      new Predicate<Storage.Objects.Insert>() {
         public boolean apply(Storage.Objects.Insert insertion) {
           assertNotNull(insertion.getMediaHttpUploader());
           assertEquals(bucket.substring(GCS_SCHEME.length()),
               insertion.getBucket());

           StorageObject object = (StorageObject) insertion.getJsonContent();

           if (sharedPublicly) {
             assertNotNull(object.getAcl());
           } else {
             assertNull(object.getAcl());
           }
           return true;
         }
      };

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(credentials.getId()).thenReturn(CREDENTIALS_ID);
    when(credentials.getProjectId()).thenReturn(PROJECT_ID);

    if (jenkins.jenkins != null) {
      SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

      // Create a project to which we may attach our uploader.
      project = jenkins.createFreeStyleProject("test");
    }

    credential = new GoogleCredential();
    when(credentials.getGoogleCredential(isA(
        GoogleOAuth2ScopeRequirement.class)))
        .thenReturn(credential);

    // Return ourselves as remotable
    when(credentials.forRemote(isA(GoogleOAuth2ScopeRequirement.class)))
        .thenReturn(credentials);

    notFoundException = new NotFoundException();
    conflictException = new ConflictException();
    forbiddenException = new ForbiddenException();

    bucket = "gs://bucket";
    glob = "bar.txt";
    sharedPublicly = false;
    forFailedJobs = false;
    showInline = false;
    stripPathPrefix = false;
    pathPrefix = null;
    underTest = new GoogleCloudStorageUploaderBuilder(CREDENTIALS_ID,
        ImmutableList.<AbstractUpload>of(
            new ClassicUpload(bucket, sharedPublicly, forFailedJobs, showInline,
                stripPathPrefix, pathPrefix, new MockUploadModule(executor),
                glob, null /* legacy arg*/, null /* legacy arg */)));
  }

  @Test
  public void testGetDefaultUploads() {
    DescriptorImpl descriptor = new DescriptorImpl();
    List<AbstractUpload> defaultUploads = descriptor.getDefaultUploads();
    assertEquals(1, defaultUploads.size());
    assertThat(defaultUploads.get(0), instanceOf(ClassicUpload.class));
  }

  private void dumpLog(Run<?, ?> run) throws IOException {
    BufferedReader reader = new BufferedReader(run.getLogReader());

    String line;
    while ((line = reader.readLine()) != null) {
      System.out.println(line);
    }
  }

  private static final String PROJECT_ID = "foo.com:bar-baz";
  private static final String CREDENTIALS_ID = "bazinga";
  private static final String NAME = "Source (foo.com:bar-baz)";
}
