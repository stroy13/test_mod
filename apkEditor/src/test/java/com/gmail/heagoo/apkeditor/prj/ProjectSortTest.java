package com.gmail.heagoo.apkeditor.prj;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ProjectSortTest {
    private static ProjectListAdapter.ItemInfo project(String name, long modified) {
        return new ProjectListAdapter.ItemInfo(name, "/apk/" + name, "/decoded/" + name, modified);
    }

    @Test
    public void newestProjectsComeFirst() {
        List<ProjectListAdapter.ItemInfo> items = new ArrayList<>(Arrays.asList(
                project("old", 100), project("new", 300), project("mid", 200)));
        ProjectListAdapter.sortProjects(items);
        assertEquals(Arrays.asList("new", "mid", "old"), Arrays.asList(
                items.get(0).name, items.get(1).name, items.get(2).name));
    }

    @Test
    public void equalTimestampsUseNaturalNameOrder() {
        List<ProjectListAdapter.ItemInfo> items = new ArrayList<>(Arrays.asList(
                project("prj10", 100), project("prj2", 100), project("prj1", 100)));
        ProjectListAdapter.sortProjects(items);
        assertEquals(Arrays.asList("prj1", "prj2", "prj10"), Arrays.asList(
                items.get(0).name, items.get(1).name, items.get(2).name));
    }
}
