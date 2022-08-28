/*
 * Copyright 2017 Aurélien Gâteau <mail@agateau.com>
 *
 * This file is part of Pixel Wheels.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.agateau.utils;

import com.agateau.utils.log.NLog;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * This class can read and write public fields of a class and serialize the changes to an xml file
 */
public class Introspector {
    public interface Listener {
        void onModified();
    }

    @SuppressWarnings("rawtypes")
    private final Class mClass;

    private final Object mReference;
    private final Object mObject;
    private final FileHandle mFileHandle;

    private final DelayedRemovalArray<Listener> mListeners = new DelayedRemovalArray<>();

    public Introspector(Object object, Object reference, FileHandle fileHandle) {
        mClass = object.getClass();
        mObject = object;
        mReference = reference;
        mFileHandle = fileHandle;
    }

    /**
     * Create an introspector using the default constructor of @p instance to create the reference
     */
    public static Introspector fromInstance(Object instance, FileHandle fileHandle) {
        Object reference;
        try {
            reference = instance.getClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                | IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("This should never happen");
        }
        return new Introspector(instance, reference, fileHandle);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void load() {
        if (!mFileHandle.exists()) {
            return;
        }
        XmlReader.Element root = FileUtils.parseXml(mFileHandle);
        if (root == null) {
            return;
        }
        for (XmlReader.Element keyElement : root.getChildrenByName("key")) {
            String name = keyElement.getAttribute("name");
            String type = keyElement.getAttribute("type");
            String value = keyElement.getText();
            Field field;
            try {
                field = mClass.getField(name);
            } catch (NoSuchFieldException e) {
                NLog.e("No field named '%s', skipping", name);
                continue;
            }
            String fieldType = field.getType().toString();
            if (!fieldType.equals(type)) {
                NLog.e(
                        "Field '%s' is of type '%s', but XML expected '%s', skipping",
                        name, fieldType, type);
                continue;
            }
            switch (type) {
                case "int":
                    set(name, Integer.valueOf(value));
                    break;
                case "boolean":
                    set(name, Boolean.valueOf(value));
                    break;
                case "float":
                    set(name, Float.valueOf(value));
                    break;
            }
        }
    }

    public void save() {
        XmlWriter writer = new XmlWriter(mFileHandle.writer(false));
        try {
            XmlWriter root = writer.element("object");
            for (Field field : mClass.getDeclaredFields()) {
                if (!Modifier.isPublic(field.getModifiers())) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value = field.get(mObject);
                if (value.equals(field.get(mReference))) {
                    continue;
                }
                root.element("key")
                        .attribute("name", field.getName())
                        .attribute("type", field.getType().toString())
                        .text(value.toString())
                        .pop();
            }
            root.pop();
            writer.close();
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public <T> T get(String key) {
        return getFrom(mObject, key);
    }

    public <T> T getReference(String key) {
        return getFrom(mReference, key);
    }

    private <T> T getFrom(Object object, String key) {
        try {
            Field field = mClass.getField(key);
            //noinspection unchecked
            return (T) field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("get(" + key + ") failed. " + e);
        }
    }

    public <T> void set(String key, T value) {
        try {
            Field field = mClass.getField(key);
            field.set(mObject, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("set(" + key + ") failed. " + e);
        }
        notifyModified();
    }

    public int getInt(String key) {
        try {
            Field field = mClass.getField(key);
            return (Integer)field.get(mObject);
//            return field.getInt(mObject);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("getInt(" + key + ") failed. " + e);
        }
    }

    public void setInt(String key, int value) {
        try {
            Field field = mClass.getField(key);
            field.set(mObject, value);
//            field.setInt(mObject, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("setInt(" + key + ") failed. " + e);
        }
        notifyModified();
    }

    public float getFloat(String key) {
        try {
            Field field = mClass.getField(key);
            return (Float)field.get(mObject);
//            return field.getFloat(mObject);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("getFloat(" + key + ") failed. " + e);
        }
    }

    public void setFloat(String key, float value) {
        try {
            Field field = mClass.getField(key);
//            field.setFloat(mObject, value);
            field.set(mObject, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("setFloat(" + key + ") failed. " + e);
        }
        notifyModified();
    }

    public boolean hasBeenModified() {
        for (Field field : mClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                if (!Objects.equals(field.get(mObject), field.get(mReference))) {
                    return true;
                }
            } catch (IllegalAccessException e) {
                // This should really not happen
                e.printStackTrace();
            }
        }
        return false;
    }

    private void notifyModified() {
        mListeners.begin();
        for (Listener listener : mListeners) {
            listener.onModified();
        }
        mListeners.end();
    }
}
