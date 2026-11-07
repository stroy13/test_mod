package com.gmail.heagoo.apkeditor.mf;

interface ISectionDeleteCallback {
    String getUndeletableReason();

    boolean sectionDeleted(LineRecord lineRecord);
}
