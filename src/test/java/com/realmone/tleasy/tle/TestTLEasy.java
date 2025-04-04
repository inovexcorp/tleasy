package com.realmone.tleasy.tle;

import com.realmone.tleasy.TLEasy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.JTextField;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class TestTLEasy {

    @Mock
    private JTextField idField;

    @Test
    public void testGetIds() throws Exception {
        Mockito.when(idField.getText()).thenReturn("12345,11111-11115,99999");
        TLEasy easy = new TLEasy();
        setField(easy, "idField", idField);
        Method m = easy.getClass().getDeclaredMethod("getIds");
        m.setAccessible(true);
        Set<String> ids = (Set<String>) m.invoke(easy);
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

    private static void setField(Object obj, String name, Object field) throws Exception {
        Field thing = obj.getClass().getDeclaredField(name);
        thing.setAccessible(true);
        thing.set(obj, field);
    }

}
