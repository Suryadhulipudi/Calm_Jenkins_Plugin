package com.calmio.calm.integration;

import com.calmio.calm.integration.Helpers.Calm;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONArray;
//import org.json.JSONObject;
import net.sf.json.JSONObject;

import java.io.OutputStream;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Suryalakshminarayana Dhulipudi
 */


public class CalmIntegrationLeader extends Builder {

    private final String bpname, appname, profname;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public CalmIntegrationLeader(String bpname, String appname, String profname) {
        this.bpname = bpname;
        this.appname = appname;
        this.profname = profname;
    }

    // We'll use this from the <tt>config.jelly</tt>.
    public String getBpname() {
       return bpname;
    }
   
    public String getAppname() {
       return appname;
    }

    public String getProfname() {
       return profname;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {
        // This is where you 'build' the project.
        PrintStream log = listener.getLogger();
        log.println("Executing Calm Integration Plugin Build Step");
        Calm call = new Calm(getDescriptor().getIP(), getDescriptor().getUser(), getDescriptor().getPwd(), getBpname(), getAppname(), getProfname());
        log.println("##Connecting to calm instance##");
        try{
		 call.launchBlueprint();
                 log.println("Blueprint " + getBpname() + " has been launched sucessfully with application name " + getAppname() + " with profile " + getProfname());
		return true;
    	}
	catch (Exception e){
		log.println(e.getMessage());
		return false;
	}
	}
		
    
    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link CalmIntegrationLeader}. Used as a singleton. The
     * class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See
     * <tt>src/main/resources/hudson/plugins//com/calmio/calm/integration/CalmIntegrationLeader/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
      
        /**
         * To persist global configuration information, simply store it in a
         * field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */

    private String calmIP, calmUser, calmPwd;

      public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
      }

      /**
       * This human readable name is used in the configuration screen.
       */
       public String getDisplayName() {
           return "Calm Integration";
       }

       @Override
       public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
           // To persist global configuration information,
           // set that to properties and call save().
           calmIP = formData.getString("calmIP");
           calmUser = formData.getString("calmUser");
           calmPwd = formData.getString("calmPwd");
           // ^Can also use req.bindJSON(this, formData);
           // (easier when there are many fields; need set* methods for this, like setUseFrench)
           save();
           return super.configure(req, formData);
    
      }

      /**
       * This method returns true if the global configuration says we should
       * speak French.
       *
       * The method trigger is bit awkward because global.jelly calls this
       * method to determine the initial state of the checkbox by the naming
       * convention.
       */
       public String getIP() {
           return calmIP;
       }
    
       public String getUser() {
           return calmUser;
       }

       public String getPwd() {
           return calmPwd;
       }
    }
   
}

