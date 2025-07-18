import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'zcs_pos_flutter_platform_interface.dart';

class MethodChannelZcsPosFlutter extends ZcsPosFlutterPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('zcs_pos_flutter');

  @override
  Future<bool> initializeSdk() async {
    final result = await methodChannel.invokeMethod<bool>('initializeSdk');
    return result ?? false;
  }

  @override
  Future<bool> printReceipt(
    String merchantName,
    String amount,
    List<String> items,
  ) async {
    final result = await methodChannel.invokeMethod<bool>('printReceipt', {
      'merchantName': merchantName,
      'amount': amount,
      'items': items,
    });
    return result ?? false;
  }

  @override
  Future<bool> showAmountOnDisplay(String amount) async {
    final result = await methodChannel.invokeMethod<bool>(
      'showAmountOnDisplay',
      {'amount': amount},
    );
    return result ?? false;
  }

  @override
  Future<Map<String, dynamic>?> readNfcCard() async {
    final result = await methodChannel.invokeMethod<Map<String, dynamic>>(
      'readNfcCard',
    );
    return result;
  }

  @override
  Future<bool> openCashDrawer() async {
    final result = await methodChannel.invokeMethod<bool>('openCashDrawer');
    return result ?? false;
  }
}
