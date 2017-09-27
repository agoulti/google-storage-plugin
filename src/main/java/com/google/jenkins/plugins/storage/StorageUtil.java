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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.jenkins.plugins.storage.AbstractUploadDescriptor.GCS_SCHEME;

import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import com.google.jenkins.plugins.storage.AbstractUpload.UploadSpec;
import com.google.jenkins.plugins.storage.reports.BuildGcsUploadReport;
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
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.Serializable;
import javax.annotation.Nullable;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A class to contain common utility operations
 */
public class StorageUtil {

  public static String processBucket(String bucket, Run<?, ?> run, TaskListener listener, String moduleName) throws InterruptedException, IOException {
    if (run instanceof AbstractBuild) {
      // Do variable name expansion only for non-pipeline builds.
      bucket = Util.replaceMacro(
          bucket, run.getEnvironment(listener));
    }

    if (!bucket.startsWith(GCS_SCHEME)) {
      listener.error(prefix(moduleName,
          Messages.AbstractUploadDescriptor_BadPrefix(
              bucket, GCS_SCHEME)));
      return "";
    }
    // Lop off the GCS_SCHEME prefix.
    bucket = bucket.substring(GCS_SCHEME.length());

    return bucket;
  }

  /**
   * Prefix the given log message with our module.
   */
  public static String prefix(String moduleName, String x) {
    return Messages.UploadModule_PrefixFormat(
        moduleName, x);
  }
}
