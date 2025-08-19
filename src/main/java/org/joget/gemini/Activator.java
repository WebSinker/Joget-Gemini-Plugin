package org.joget.gemini;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        try {
            // Create plugin instance
            GeminiPlugin plugin = new GeminiPlugin();

            // Create service properties using Dictionary (OSGi standard)
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("service.description", "Gemini AI Plugin with Embedded HTTP Server and Database Integration");
            props.put("plugin.class", GeminiPlugin.class.getName());
            props.put("plugin.version", "2.3.0"); // Updated to match new plugin version
            props.put("embedded.server.port", "8081");
            props.put("database.enabled", "true");

            // Register plugin services
            registrationList.add(context.registerService(GeminiPlugin.class.getName(), plugin, props));
            registrationList.add(context.registerService("org.joget.plugin.base.Plugin", plugin, props));
            registrationList.add(context.registerService("org.joget.plugin.base.ApplicationPlugin", plugin, props));

            // Start embedded HTTP server
            GeminiPlugin.startEmbeddedServer();

            // Test database connection during startup
            boolean databaseConnected = false;
            try {
                databaseConnected = DatabaseService.testConnection();
            } catch (Exception dbError) {
                System.err.println("Database connection test failed: " + dbError.getMessage());
            }

            // Success logging
            System.out.println("==========================================");
            System.out.println("=== Gemini Plugin Bundle Started ===");
            System.out.println("==========================================");
            System.out.println("Plugin: " + plugin.getLabel() + " v" + plugin.getVersion());
            System.out.println("Class: " + plugin.getClassName());
            System.out.println("");
            System.out.println("Registered Services:");
            System.out.println("✓ " + GeminiPlugin.class.getName() + " (Process Tool)");
            System.out.println("✓ org.joget.plugin.base.Plugin (Base Plugin)");
            System.out.println("✓ org.joget.plugin.base.ApplicationPlugin (App Plugin)");
            System.out.println("");
            System.out.println("🚀 EMBEDDED HTTP SERVER:");
            System.out.println("• Port: " + GeminiPlugin.getEmbeddedServerPort());
            System.out.println("• Status: " + (GeminiPlugin.isEmbeddedServerRunning() ? "RUNNING ✅" : "STOPPED ❌"));
            System.out.println("");
            System.out.println("🤖 AI CHAT ENDPOINTS:");
            System.out.println("• Chat API: http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/chat");
            System.out.println("• Health API: http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/health");
            System.out.println("• Test API: http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/test");
            System.out.println("");
            System.out.println("🗄️ DATABASE ENDPOINTS:");
            System.out.println("• Connection: " + (databaseConnected ? "CONNECTED ✅" : "DISCONNECTED ❌"));
            System.out.println("• DB Test: http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/db/test");
            System.out.println("• DB Apps: http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/db/apps");
            System.out.println("• DB Forms: http://localhost:" + GeminiPlugin.getEmbeddedServerPort()
                    + "/db/forms?appId=APP&appVersion=1");
            System.out.println(
                    "• DB Users: http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/db/users?search=TERM");
            System.out.println("• Chat History: http://localhost:" + GeminiPlugin.getEmbeddedServerPort()
                    + "/db/chat-history?sessionId=SESSION");
            System.out.println("• DB Info: http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/db/info");
            System.out.println("");
            System.out.println("📖 DOCUMENTATION:");
            System.out.println("• API Docs: http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/");
            System.out.println("");
            System.out.println("🧪 QUICK TESTS:");
            System.out.println("# Basic functionality");
            System.out.println("curl \"http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/test\"");
            System.out.println("curl \"http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/health\"");
            System.out.println(
                    "curl \"http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/chat?userPrompt=Hello\"");
            System.out.println("");
            System.out.println("# Database functionality");
            System.out.println("curl \"http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/db/test\"");
            System.out.println("curl \"http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/db/apps\"");
            System.out.println(
                    "curl \"http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/db/users?search=admin\"");
            System.out.println("");
            System.out.println("# Chat with database logging");
            System.out
                    .println("curl -X POST \"http://localhost:" + GeminiPlugin.getEmbeddedServerPort() + "/chat\" \\");
            System.out.println("  -d \"userPrompt=Hello&sessionId=test123&saveToDb=true\"");
            System.out.println("");
            System.out.println("📊 SUMMARY:");
            System.out.println("• Total registrations: " + registrationList.size());
            System.out.println("• Bundle context: " + context.getBundle().getSymbolicName());
            System.out.println("• Server: " + (GeminiPlugin.isEmbeddedServerRunning() ? "Running" : "Stopped"));
            System.out.println("• Database: " + (databaseConnected ? "Connected" : "Disconnected"));
            System.out.println("==========================================");

            // Warning if database is not connected
            if (!databaseConnected) {
                System.out.println("⚠️  WARNING: Database connection failed!");
                System.out.println("   Database features will not work properly.");
                System.out.println("   Please check your database configuration in DatabaseService.java");
                System.out.println("   - Host: localhost:3307");
                System.out.println("   - Database: jwdb");
                System.out.println("   - User: root");
                System.out.println("==========================================");
            }

        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("=== ERROR: Failed to start Gemini Plugin Bundle ===");
            System.err.println("==========================================");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();
            System.err.println("==========================================");

            // Stop embedded server if it was started
            try {
                GeminiPlugin.stopEmbeddedServer();
            } catch (Exception stopError) {
                System.err.println("Error stopping embedded server: " + stopError.getMessage());
            }

            // Clean up any partial registrations
            if (registrationList != null) {
                for (ServiceRegistration registration : registrationList) {
                    try {
                        registration.unregister();
                    } catch (Exception cleanupError) {
                        System.err.println("Error during cleanup: " + cleanupError.getMessage());
                    }
                }
                registrationList.clear();
            }

            throw new RuntimeException("Failed to start Gemini Plugin Bundle", e);
        }
    }

    public void stop(BundleContext context) {
        System.out.println("==========================================");
        System.out.println("=== Stopping Gemini Plugin Bundle ===");
        System.out.println("==========================================");

        // Stop embedded HTTP server first
        try {
            GeminiPlugin.stopEmbeddedServer();
            System.out.println("✅ Embedded HTTP server stopped");
        } catch (Exception e) {
            System.err.println("❌ Error stopping embedded server: " + e.getMessage());
        }

        // Unregister services
        if (registrationList != null) {
            int unregistered = 0;
            for (ServiceRegistration registration : registrationList) {
                try {
                    registration.unregister();
                    unregistered++;
                } catch (Exception e) {
                    System.err.println("Error unregistering service: " + e.getMessage());
                }
            }
            registrationList.clear();

            System.out.println("✅ Unregistered " + unregistered + " services");
        }

        System.out.println("✅ Gemini Plugin Bundle stopped successfully");
        System.out.println("==========================================");
    }
}