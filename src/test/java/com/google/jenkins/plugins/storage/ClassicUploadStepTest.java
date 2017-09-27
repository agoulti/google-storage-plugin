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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.storage.ClassicUpload.DescriptorImpl;
import com.google.jenkins.plugins.util.ConflictException;
import com.google.jenkins.plugins.util.ForbiddenException;
import com.google.jenkins.plugins.util.MockExecutor;
import com.google.jenkins.plugins.util.NotFoundException;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.util.FormValidation;
import java.io.BufferedReader;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Tests for {@link ClassicUpload}.
 */
public class ClassicUploadStepTest {
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Mock
  private GoogleRobotCredentials credentials;
  private GoogleCredential credential;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(credentials.getId()).thenReturn(CREDENTIALS_ID);
    when(credentials.getProjectId()).thenReturn(PROJECT_ID);

    if (jenkins.jenkins != null) {
      SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
    }
/*
    credential = new GoogleCredential();
    when(credentials.getGoogleCredential(isA(
        GoogleOAuth2ScopeRequirement.class)))
        .thenReturn(credential);
    when(credentials.forRemote(isA(GoogleOAuth2ScopeRequirement.class)))
        .thenReturn(credentials);
        */
  }

  private void ConfigurationRoundTripTest(ClassicUploadStep s) throws Exception {
    ClassicUploadStep after = jenkins.configRoundtrip(s);
    jenkins.assertEqualBeans(s, after, "bucket,pattern,pathPrefix"); // credentialsId
  }

  @Test
  public void testRoundtrip() throws Exception {
    ClassicUploadStep step = new ClassicUploadStep(CREDENTIALS_ID, "bucket", "pattern");
    ConfigurationRoundTripTest(step);

    step.setPathPrefix("prefix");
    ConfigurationRoundTripTest(step);

    step.setSharedPublicly(true);
    ConfigurationRoundTripTest(step);

    step.setShowInline(true);
    ConfigurationRoundTripTest(step);
  }

  private static final String PROJECT_ID = "foo.com:project-build";
  private static final String CREDENTIALS_ID = "builderStep";
}
