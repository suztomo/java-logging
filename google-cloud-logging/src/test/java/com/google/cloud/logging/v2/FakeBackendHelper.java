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

import com.google.api.core.ApiFunction;
import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.batching.FlowController.LimitExceededBehavior;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.MonitoredResource;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.StringPayload;
import com.google.cloud.logging.Severity;
import com.google.cloud.logging.v2.stub.GrpcLoggingServiceV2Stub;
import com.google.cloud.logging.v2.stub.LoggingServiceV2StubSettings;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.Collections;

public class FakeBackendHelper {
  public static void main(String[] args) {
    int port = 8998;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }
    try {
      Server server = ServerBuilder.forPort(port).addService(new MockedLoggingBackend()).build();
      server.start();

      // Create a logging client that conencts to the fake server
      GrpcTransportOptions.Builder builder = GrpcTransportOptions.newBuilder();

      LoggingServiceV2StubSettings.Builder settings =
          LoggingServiceV2StubSettings.newBuilder()
              .setEndpoint("localhost:" + String.valueOf(port))
              .setTransportChannelProvider(
                  InstantiatingGrpcChannelProvider.newBuilder()
                      .setChannelConfigurator(
                          new ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder>() {
                            @Override
                            public ManagedChannelBuilder apply(ManagedChannelBuilder builder) {
                              return builder.usePlaintext();
                            }
                          })
                      .build());

      // Configure the batching settings so that flow controller blocks after exactly 10 elements
      settings
          .writeLogEntriesSettings()
          .setBatchingSettings(
              BatchingSettings.newBuilder()
                  .setElementCountThreshold(10l)
                  .setIsEnabled(true)
                  .setFlowControlSettings(
                      FlowControlSettings.newBuilder()
                          // important
                          .setLimitExceededBehavior(LimitExceededBehavior.Block)
                          .setMaxOutstandingElementCount(10l)
                          .build())
                  .build());

      // create the stub
      GrpcLoggingServiceV2Stub stub = GrpcLoggingServiceV2Stub.create(settings.build());

      System.out.printf("Start sending logs\n");

      Logging logging = LoggingOptions.newBuilder().setTransportOptions(stub).build().getService();

      // The name of the log to write to
      String logName = "dummy-log";

      // The data to write to the log
      String text =
          "Meaningless log payload. Following symbols complete payload to 1KB:"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде"
              + "0123456789abcdefghijklmnopqrstuwxyz!?+-%/=.,<>@#&^()_:;[]{}|$абвгде";

      LogEntry entry =
          LogEntry.newBuilder(StringPayload.of(text))
              .setSeverity(Severity.INFO)
              .setLogName(logName)
              .setResource(MonitoredResource.newBuilder("global").build())
              .build();

      // Writes the log entry asynchronously
      for (int i = 0; i < 15; i++) {
        logging.write(Collections.singleton(entry));
      }
      System.out.printf("Finished sending logs\n");
    } catch (IOException e) {
      // do nothing
    }
  }
}
