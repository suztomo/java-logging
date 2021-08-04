/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.logging.v2;

import com.google.logging.v2.LoggingServiceV2Grpc;
import com.google.logging.v2.WriteLogEntriesRequest;
import com.google.logging.v2.WriteLogEntriesResponse;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MockedLoggingBackend extends LoggingServiceV2Grpc.LoggingServiceV2ImplBase {
  private ConcurrentLinkedQueue<StreamObserver<WriteLogEntriesResponse>> responseStreams =
      new ConcurrentLinkedQueue<>();

  @Override
  public void writeLogEntries(
      WriteLogEntriesRequest req, StreamObserver<WriteLogEntriesResponse> respObserver) {
    responseStreams.add(respObserver);

    if (responseStreams.size() > 10) {
      for (StreamObserver<WriteLogEntriesResponse> responseStream : responseStreams) {
        responseStream.onError(Code.UNAVAILABLE.toStatus().asRuntimeException());
      }
      responseStreams.clear();
    }
  }
}
