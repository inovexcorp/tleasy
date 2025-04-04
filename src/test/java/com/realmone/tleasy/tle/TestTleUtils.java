package com.realmone.tleasy.tle;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class TestTleUtils {

    @Test
    public void testGetIds() {
        Set<String> ids = TleUtils.parseIdentifiers("12345,11111-11115,99999");
        Set<String> expected = new HashSet<>();
        expected.add("12345");
        expected.add("11111");
        expected.add("11112");
        expected.add("11113");
        expected.add("11114");
        expected.add("11115");
        expected.add("99999");
        Assert.assertEquals(expected.size(), ids.size());
        for (String id : ids) {
            Assert.assertTrue(expected.contains(id));
        }
    }

    @Test
    public void testNumbersBetween() {
        Set<Integer> numbers = TleUtils.getNumbersBetween(11111, 11113);
        Set<Integer> expected = new HashSet<>();
        expected.add(11111);
        expected.add(11112);
        expected.add(11113);
        Assert.assertEquals("Size mismatch", expected.size(), numbers.size());
        for (int found : numbers) {
            Assert.assertTrue("One of our output isn't expected", expected.contains(found));
        }
    }
}
