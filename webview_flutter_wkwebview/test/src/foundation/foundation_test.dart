// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:webview_flutter_wkwebview/src/common/instance_manager.dart';
import 'package:webview_flutter_wkwebview/src/common/web_kit.pigeon.dart';
import 'package:webview_flutter_wkwebview/src/foundation/foundation.dart';
import 'package:webview_flutter_wkwebview/src/foundation/foundation_api_impls.dart';

import '../common/test_web_kit.pigeon.dart';
import 'foundation_test.mocks.dart';

@GenerateMocks(<Type>[
  TestNSObjectHostApi,
])
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Foundation', () {
    late InstanceManager instanceManager;

    setUp(() {
      instanceManager = InstanceManager(onWeakReferenceRemoved: (_) {});
    });

    group('NSObject', () {
      late MockTestNSObjectHostApi mockPlatformHostApi;

      late NSObject object;

      setUp(() {
        mockPlatformHostApi = MockTestNSObjectHostApi();
        TestNSObjectHostApi.setup(mockPlatformHostApi);

        object = NSObject.detached(instanceManager: instanceManager);
        instanceManager.addDartCreatedInstance(object);
      });

      tearDown(() {
        TestNSObjectHostApi.setup(null);
      });

      test('addObserver', () async {
        final NSObject observer = NSObject.detached(
          instanceManager: instanceManager,
        );
        instanceManager.addDartCreatedInstance(observer);

        await object.addObserver(
          observer,
          keyPath: 'aKeyPath',
          options: <NSKeyValueObservingOptions>{
            NSKeyValueObservingOptions.initialValue,
            NSKeyValueObservingOptions.priorNotification,
          },
        );

        final List<NSKeyValueObservingOptionsEnumData?> optionsData =
            verify(mockPlatformHostApi.addObserver(
          instanceManager.getIdentifier(object),
          instanceManager.getIdentifier(observer),
          'aKeyPath',
          captureAny,
        )).captured.single as List<NSKeyValueObservingOptionsEnumData?>;

        expect(optionsData, hasLength(2));
        expect(
          optionsData[0]!.value,
          NSKeyValueObservingOptionsEnum.initialValue,
        );
        expect(
          optionsData[1]!.value,
          NSKeyValueObservingOptionsEnum.priorNotification,
        );
      });

      test('removeObserver', () async {
        final NSObject observer = NSObject.detached(
          instanceManager: instanceManager,
        );
        instanceManager.addDartCreatedInstance(observer);

        await object.removeObserver(observer, keyPath: 'aKeyPath');

        verify(mockPlatformHostApi.removeObserver(
          instanceManager.getIdentifier(object),
          instanceManager.getIdentifier(observer),
          'aKeyPath',
        ));
      });

      test('NSObjectHostApi.dispose', () async {
        int? callbackIdentifier;
        final InstanceManager instanceManager =
            InstanceManager(onWeakReferenceRemoved: (int identifier) {
          callbackIdentifier = identifier;
        });

        final NSObject object = NSObject.detached(
          instanceManager: instanceManager,
        );
        final int identifier = instanceManager.addDartCreatedInstance(object);

        NSObject.dispose(object);
        expect(callbackIdentifier, identifier);
      });

      test('observeValue', () async {
        final Completer<List<Object?>> argsCompleter =
            Completer<List<Object?>>();

        FoundationFlutterApis.instance = FoundationFlutterApis(
          instanceManager: instanceManager,
        );

        object = NSObject.detached(
          instanceManager: instanceManager,
          observeValue: (
            String keyPath,
            NSObject object,
            Map<NSKeyValueChangeKey, Object?> change,
          ) {
            argsCompleter.complete(<Object?>[keyPath, object, change]);
          },
        );
        instanceManager.addHostCreatedInstance(object, 1);

        FoundationFlutterApis.instance.object.observeValue(
          1,
          'keyPath',
          1,
          <NSKeyValueChangeKeyEnumData>[
            NSKeyValueChangeKeyEnumData(value: NSKeyValueChangeKeyEnum.oldValue)
          ],
          <Object?>['value'],
        );

        expect(
          argsCompleter.future,
          completion(<Object?>[
            'keyPath',
            object,
            <NSKeyValueChangeKey, Object?>{
              NSKeyValueChangeKey.oldValue: 'value',
            },
          ]),
        );
      });

      test('NSObjectFlutterApi.dispose', () {
        FoundationFlutterApis.instance = FoundationFlutterApis(
          instanceManager: instanceManager,
        );

        object = NSObject.detached(instanceManager: instanceManager);
        instanceManager.addHostCreatedInstance(object, 1);

        instanceManager.removeWeakReference(object);
        FoundationFlutterApis.instance.object.dispose(1);

        expect(instanceManager.containsIdentifier(1), isFalse);
      });
    });
  });
}
