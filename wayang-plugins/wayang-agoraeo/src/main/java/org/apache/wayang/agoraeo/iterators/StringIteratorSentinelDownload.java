package org.apache.wayang.agoraeo.iterators;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

public class StringIteratorSentinelDownload extends IteratorSentinelDownload<String> {

    public StringIteratorSentinelDownload(String name, String command) {
        super(name, command);
    }

    @Override
    protected Stream<String> getLogic(Stream<String> baseline) {
        return baseline;
    }

    @Override
    protected String getDefaultValue() {
        return "";
    }
}
