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

import com.google.common.base.Objects;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.util.Resolve;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * This upload extension implements the classical upload pattern
 * where a user provides an Ant-style glob, e.g. ** / *.java
 * relative to the build workspace, and those files are uploaded
 * to the storage bucket.
 */
public class ClassicUploadStep extends Builder implements SimpleBuildStep, Serializable {
  @Nonnull
  protected ClassicUpload upload;

  /**
   * Construct the classic upload step. See ClassicUpload documentation for parameter descriptions.
   */
  @DataBoundConstructor
  public ClassicUploadStep(String credentialsId, String bucket, boolean sharedPublicly,
      boolean showInline, boolean stripPathPrefix,
      @Nullable String pathPrefix, @Nullable UploadModule module,
      String pattern) {
    this.credentialsId = credentialsId;
    upload = new ClassicUpload(bucket, sharedPublicly, false /* forFailedJobs */, showInline, stripPathPrefix, pathPrefix, module, pattern, null, null);
  }

  /**
   * The unique ID for the credentials we are using to
   * authenticate with GCS.
   */
  public String getCredentialsId() {
    return credentialsId;
  }
  private final String credentialsId;

  @Override
  public void perform(Run<?,?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws IOException {
    try {
      upload.perform(GoogleRobotCredentials.getById(getCredentialsId()), run, workspace, listener);
    } catch(UploadException e) {
      throw new IOException("Could not perform upload", e);
    }
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return Messages.ClassicUpload_BuildStepDisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }
  }
}
