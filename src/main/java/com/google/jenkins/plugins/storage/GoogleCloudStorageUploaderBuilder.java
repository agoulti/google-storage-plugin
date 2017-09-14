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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.jenkins.plugins.storage.AbstractUploadDescriptor.GCS_SCHEME;

import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.storage.reports.ProjectGcsUploadReport;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.tasks.Builder;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A build step wrapper for the Google Plugin Storage (GCS) plugin.
 */
@RequiresDomain(value = StorageScopeRequirement.class)
public class GoogleCloudStorageUploaderBuilder extends Builder {
  /**
   * Construct the GCS uploader to use the provided credentials to
   * upload build artifacts.
   *
   * @param credentialsId The credentials to utilize for authenticating with GCS
   * @param uploads The list of uploads the user has requested be done
   */

  private GoogleCloudStorageUploader uploader;

  @DataBoundConstructor
  public GoogleCloudStorageUploaderBuilder(
      String credentialsId, @Nullable List<AbstractUpload> uploads) {
    uploader = new GoogleCloudStorageUploader(credentialsId, uploads);
  }

  /**
   * The unique ID for the credentials we are using to
   * authenticate with GCS.
   */
  public String getCredentialsId() {
    return uploader.getCredentialsId();
  }


  /**
   * The set of tuples describing the artifacts to upload, and where
   * to upload them.
   */
  public Collection<AbstractUpload> getUploads() {
    return uploader.getUploads();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
      BuildListener listener) throws IOException, InterruptedException {
    return uploader.perform(build, launcher, listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  /**
   * Descriptor for the extension for uploading build artifacts to
   * Google Cloud Storage.
   */
  @Extension
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Builder> {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(
        @SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return Messages.GoogleCloudStorageUploader_DisplayName();
    }

    /**
     * @return the default uploads when the user configure
     *         {@link GoogleCloudStorageUploaderBuilder} for the first time.
     */
    public List<AbstractUpload> getDefaultUploads() {
      return ImmutableList.<AbstractUpload>of(
          new ClassicUpload(GCS_SCHEME,
              false /* public? */, false /* for failed? */,
              false /* show inline? */,
              false /* strip path prefix? */,
              null /* path prefix */,
              null /* module */, "" /* pattern */,
              null /* legacy arg: bucketNameWithVars */,
              null /* legacy arg: sourceGlobWithVars */));
    }

    public List<AbstractUploadDescriptor> getUploads() {
      return AbstractUpload.all();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new ProjectGcsUploadReport(project);
  }
}
