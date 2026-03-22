import 'dart:typed_data';

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

  Future<bool> printReceipt({ required String merchantName,
    required String billerName,
    required List<Map<String,dynamic>> items,
    required String netAmount,
    required String cashPaid,}
  ) {
    throw UnimplementedError('printReceipt() has not been implemented.');
  }  Future<bool> printBigText({ required String text,

  }
  ) {
    throw UnimplementedError('printBigText() has not been implemented.');
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
  Future<bool> printBarcode({required String text}) {
    throw UnimplementedError('printBarcode() has not been implemented.');
  }

  Future<bool> startScanner() {
    throw UnimplementedError('startScanner() has not been implemented.');
  }

  Future<bool> stopScanner() {
    throw UnimplementedError('stopScanner() has not been implemented.');
  }

  Future<String> printHtmlContent(String htmlContent) {
    throw UnimplementedError('printHtmlContent() has not been implemented.');
  } Future<String> printPdfFromPathOrAsset(String  path,bool isAsset) {
    throw UnimplementedError('printHtmlContent() has not been implemented.');
  }
  Future<String> printPdfFromUrl(String  pdfUrl,) {
    throw UnimplementedError('printPdfFromUrl() has not been implemented.');
  }
}
