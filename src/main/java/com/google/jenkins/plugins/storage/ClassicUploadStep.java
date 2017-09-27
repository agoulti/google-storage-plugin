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

import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.util.Resolve;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * This upload extension implements the classical upload pattern
 * where a user provides an Ant-style glob, e.g. ** / *.java
 * relative to the build workspace, and those files are uploaded
 * to the storage bucket.
 */
@RequiresDomain(value = StorageScopeRequirement.class)
public class ClassicUploadStep extends Builder implements SimpleBuildStep, Serializable {
  @Nonnull
  protected ClassicUpload upload;

  @DataBoundConstructor
  public ClassicUploadStep(String credentialsId, String bucket, String pattern) {
    this.credentialsId = credentialsId;
    upload = new ClassicUpload(bucket, null, pattern, null, null);
  }

  /**
   * Construct the classic upload step. See ClassicUpload documentation for parameter descriptions.
   */
  public ClassicUploadStep(String credentialsId, String bucket, @Nullable UploadModule module,
      String pattern) {
    this.credentialsId = credentialsId;
    upload = new ClassicUpload(bucket, null, pattern, null, null);
  }

  /**
   * Whether to surface the file being uploaded to anyone with the link.
   */
  @DataBoundSetter
  public void setSharedPublicly(boolean sharedPublicly) {
    upload.setSharedPublicly(sharedPublicly);
  }
  public boolean isSharedPublicly() {
    return upload.isSharedPublicly();
  }

  /**
   * Whether to indicate in metadata that the file should be viewable inline
   * in web browsers, rather than requiring it to be downloaded first.
   */
  @DataBoundSetter
  public void setShowInline(boolean showInline) {
    upload.setShowInline(showInline);
  }
  public boolean isShowInline() {
    return upload.isShowInline();
  }


  /**
   * The path prefix that will be stripped from uploaded files. May be null
   * if no path prefix needs to be stripped.
   *
   * Filenames that do not start with this prefix will not be modified. Trailing slash is
   * automatically added if it is missing.
   */
  @DataBoundSetter
  public void setPathPrefix(@Nullable String pathPrefix) {
    upload.setPathPrefix(pathPrefix);
  }
  @Nullable
  public String getPathPrefix() {
    return upload.getPathPrefix();
  }

  public String getPattern() {
    return upload.getPattern();
  }

  public String getBucket() {
    return upload.getBucket();
  }

  /**
   * The unique ID for the credentials we are using to
   * authenticate with GCS.
   */
  @DataBoundSetter
  public void setCredentialsId(String credentialsId) {
    this.credentialsId = credentialsId;
  }
  public String getCredentialsId() {
    return credentialsId;
  }
  private String credentialsId;

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public void perform(Run<?,?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws IOException {
    try {
      upload.perform(GoogleRobotCredentials.getById(getCredentialsId()), run, workspace, listener);
    } catch(UploadException e) {
      throw new IOException("Could not perform upload", e);
    }
  }

  @Extension @Symbol("googleStorageUpload")
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

    @Override
    public Builder newInstance(StaplerRequest req, JSONObject formData)
        throws FormException {
      if (Boolean.FALSE.equals(formData.remove("stripPathPrefix"))) {
        formData.remove("pathPrefix");
      }
      return super.newInstance(req, formData);
    }

    /**
     * This callback validates the {@code bucketNameWithVars} input field's
     * values.
     */
    public FormValidation doCheckBucket(
        @QueryParameter final String bucket)
        throws IOException {
      return ClassicUpload.DescriptorImpl.staticDoCheckBucketNameWithVars(bucket);
    }

    public static FormValidation doCheckPattern(
        @QueryParameter final String pattern)
        throws IOException {
      return ClassicUpload.DescriptorImpl.staticDoCheckPattern(pattern);
    }
  }
}
