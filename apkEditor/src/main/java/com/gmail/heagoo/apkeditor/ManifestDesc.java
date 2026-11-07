package com.gmail.heagoo.apkeditor;

import java.util.HashMap;
import java.util.Map;

class ManifestDesc {
    private static Map<String, String> tag2Desc = new HashMap();

    ManifestDesc() {
    }

    static {
        tag2Desc.put("action", "Adds an action to an intent filter.");
        tag2Desc.put("activity", "Declares an activity that implements part of the application's visual user interface.");
        tag2Desc.put("activity-alias", "An alias for an activity.");
        tag2Desc.put("application", "The declaration of the application. This element contains subelements that declare each of the application's components and has attributes that can affect all the components.");
        tag2Desc.put("category", "Adds a category name to an intent filter.");
        tag2Desc.put("compatible-screens", "Specifies each screen configuration with which the application is compatible.");
        tag2Desc.put("data", "Adds a data specification to an intent filter.");
        tag2Desc.put("grant-uri-permission", "Specifies which data subsets of the parent content provider permission can be granted for.");
        tag2Desc.put("instrumentation", "Declares an Instrumentation class that enables you to monitor an application's interaction with the system.");
        tag2Desc.put("intent-filter", "Specifies the types of intents that an activity, service, or broadcast receiver can respond to. An intent filter declares the capabilities of its parent component -- what an activity or service can do and what types of broadcasts a receiver can handle.");
        tag2Desc.put("manifest", "The root element of the AndroidManifest.xml file.");
        tag2Desc.put("meta-data", "A name-value pair for an item of additional, arbitrary data that can be supplied to the parent component.");
        tag2Desc.put("path-permission", "Defines the path and required permissions for a specific subset of data within a content provider.");
        tag2Desc.put("permission", "Declares a security permission that can be used to limit access to specific components or features of this or other applications.");
        tag2Desc.put("permission-group", "Declares a name for a logical grouping of related permissions.");
        tag2Desc.put("permission-tree", "Declares the base name for a tree of permissions.");
        tag2Desc.put("provider", "Declares a content provider component. A content provider supplies structured access to data managed by the application.");
        tag2Desc.put("receiver", "Declares a broadcast receiver as one of the application's components. Broadcast receivers enable applications to receive intents that are broadcast by the system or by other applications, even when other components of the application are not running.");
        tag2Desc.put("service", "Declares a service as one of the application's components. Unlike activities, services lack a visual user interface. They're used to implement long-running background operations");
        tag2Desc.put("supports-gl-texture", "Declares a single GL texture compression format that is supported by the application.");
        tag2Desc.put("supports-screens", "Lets you specify the screen sizes your application supports and enable screen compatibility mode for screens larger than what your application supports.");
        tag2Desc.put("uses-configuration", "Indicates what hardware and software features the application requires.");
        tag2Desc.put("uses-feature", "Declares a single hardware or software feature that is used by the application.");
        tag2Desc.put("uses-library", "Specifies a shared library that the application must be linked against.");
        tag2Desc.put("uses-permission", "Requests a permission that the application must be granted in order for it to operate correctly.");
        tag2Desc.put("uses-sdk", "Lets you express an application's compatibility with one or more versions of the Android platform, by means of an API Level integer.");
    }

    static String getDescription(String tag) {
        return tag2Desc.get(tag);
    }
}
