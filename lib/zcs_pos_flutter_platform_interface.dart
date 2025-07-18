import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'zcs_pos_flutter_method_channel.dart';

abstract class ZcsPosFlutterPlatform extends PlatformInterface {
  ZcsPosFlutterPlatform() : super(token: _token);

  static final Object _token = Object();
  static ZcsPosFlutterPlatform _instance = MethodChannelZcsPosFlutter();

  static ZcsPosFlutterPlatform get instance => _instance;

  Future<bool> initializeSdk() {
    throw UnimplementedError('initializeSdk() has not been implemented.');
  }

  Future<bool> printReceipt(
    String merchantName,
    String amount,
    List<String> items,
  ) {
    throw UnimplementedError('printReceipt() has not been implemented.');
  }

  Future<bool> showAmountOnDisplay(String amount) {
    throw UnimplementedError('showAmountOnDisplay() has not been implemented.');
  }

  Future<Map<String, dynamic>?> readNfcCard() {
    throw UnimplementedError('readNfcCard() has not been implemented.');
  }

  Future<bool> openCashDrawer() {
    throw UnimplementedError('openCashDrawer() has not been implemented.');
  }
}
