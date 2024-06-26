/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.hadoop.fileSystem;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ChecksumFileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.tsfile.read.reader.TsFileInput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class HDFSInput implements TsFileInput {

  private FSDataInputStream fsDataInputStream;
  private FileSystem fs;
  private Path path;

  public HDFSInput(String filePath) throws IOException {
    this(new Path(filePath), new Configuration());
  }

  public HDFSInput(String filePath, Configuration configuration) throws IOException {
    this(new Path(filePath), configuration);
  }

  public HDFSInput(Path path, Configuration configuration) throws IOException {
    this.fs = path.getFileSystem(configuration);
    this.path = path;
    this.fsDataInputStream = fs.open(path);
  }

  @Override
  public long size() throws IOException {
    return fs.getFileStatus(path).getLen();
  }

  @Override
  public long position() throws IOException {
    return fsDataInputStream.getPos();
  }

  @Override
  public synchronized TsFileInput position(long newPosition) throws IOException {
    fsDataInputStream.seek(newPosition);
    return this;
  }

  @Override
  public synchronized int read(ByteBuffer dst) throws IOException {
    int res;
    if (fs instanceof ChecksumFileSystem) {
      byte[] bytes = new byte[dst.remaining()];
      res = fsDataInputStream.read(bytes);
      dst.put(bytes);
    } else {
      res = fsDataInputStream.read(dst);
    }
    return res;
  }

  @Override
  public synchronized int read(ByteBuffer dst, long position) throws IOException {
    if (position < 0) {
      throw new IllegalArgumentException("position must be non-negative");
    }

    if (position >= this.size()) {
      return -1;
    }

    long srcPosition = fsDataInputStream.getPos();

    fsDataInputStream.seek(position);
    int res = read(dst);
    fsDataInputStream.seek(srcPosition);

    return res;
  }

  @Override
  public InputStream wrapAsInputStream() {
    return fsDataInputStream;
  }

  @Override
  public void close() throws IOException {
    fsDataInputStream.close();
  }

  @Override
  public String getFilePath() {
    return path.toString();
  }
}
