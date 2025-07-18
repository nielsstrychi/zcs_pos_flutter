import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:zcs_pos_flutter/zcs_pos_flutter_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelZcsPosFlutter platform = MethodChannelZcsPosFlutter();
  const MethodChannel channel = MethodChannel('zcs_pos_flutter');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
          return '42';
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });
}
