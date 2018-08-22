/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialfilemanager.filesystem;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import org.threeten.bp.Instant;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import eu.chainfire.libsuperuser.Shell;
import me.zhanghai.android.materialfilemanager.R;
import me.zhanghai.android.materialfilemanager.functional.Functional;
import me.zhanghai.android.materialfilemanager.functional.FunctionalException;
import me.zhanghai.android.materialfilemanager.functional.throwing.ThrowingFunction;

public class ShellLocalFile extends LocalFile {

    private Stat.Information mInformation;

    public ShellLocalFile(Uri uri) {
        super(uri);
    }

    private ShellLocalFile(Uri uri, Stat.Information information) {
        super(uri);

        mInformation = information;
    }

    @WorkerThread
    public void loadInformation() throws FileSystemException {
        String command = Stat.makeCommand(mUri.getPath());
        List<String> outputs = Shell.SH.run(command);
        if (outputs == null) {
            // TODO
        }
        mInformation = Stat.parseOutput(outputs.get(0));
    }

    @Override
    public long getSize() {
        return mInformation.size;
    }

    @Override
    public Instant getLastModificationTime() {
        return mInformation.lastModificationTime;
    }

    @NonNull
    public PosixFileType getType() {
        return mInformation.type;
    }

    @Override
    public boolean isDirectory() {
        return mInformation.type == PosixFileType.DIRECTORY;
    }

    @Override
    @WorkerThread
    public List<File> getChildren() throws FileSystemException {
        List<java.io.File> javaFiles = Arrays.asList(makeJavaFile().listFiles());
        List<String> paths = Functional.map(javaFiles, java.io.File::getPath);
        // TODO: ARG_MAX
        String command = Stat.makeCommand(paths);
        List<String> outputs = Shell.SH.run(command);
        if (outputs == null) {
            throw new FileSystemException(R.string.file_list_error_directory);
        }
        // TODO: Size mismatch.
        List<Stat.Information> informations;
        try {
            informations = Functional.map(outputs, (ThrowingFunction<String, Stat.Information>)
                    Stat::parseOutput);
        } catch (FunctionalException e) {
            throw e.getCauseAs(FileSystemException.class);
        }
        return Functional.map(javaFiles, (javaFile, index) -> new ShellLocalFile(
                Uri.fromFile(javaFile), informations.get(index)));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ShellLocalFile that = (ShellLocalFile) object;
        return Objects.equals(mUri, that.mUri)
                && Objects.equals(mInformation, that.mInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mUri, mInformation);
    }


    public static final Creator<ShellLocalFile> CREATOR = new Creator<ShellLocalFile>() {
        @Override
        public ShellLocalFile createFromParcel(Parcel source) {
            return new ShellLocalFile(source);
        }

        @Override
        public ShellLocalFile[] newArray(int size) {
            return new ShellLocalFile[size];
        }
    };

    protected ShellLocalFile(Parcel in) {
        super(in);

        mInformation = in.readParcelable(Stat.Information.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeParcelable(mInformation, flags);
    }
}
