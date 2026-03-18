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
  Future<bool> printReceipt({required String merchantName, required String billerName, required List<Map<String, dynamic>> items, required String netAmount, required String cashPaid}) async{
    // TODO: implement printReceipt
    final bool result = await methodChannel.invokeMethod('printReceipt', {
      'merchantName': merchantName,
      'billerName': billerName,
      'items': items,
      'netAmount': netAmount,
      'cashPaid': cashPaid,
    });

    return result;
  }

  @override
  Future<bool> printBigText({required String text})async {
    final bool result  = await methodChannel.invokeMethod('printBigText',{
      "text":text
    });
    return result;
  }  @override
  Future<bool> printBarcode({required String text})async {
    final bool result  = await methodChannel.invokeMethod('printBarcode',{
      "text":text
    });
    return result;
  }

  @override
  Future<String?> scanBarcode() async {
    final result = await methodChannel.invokeMethod<String>('scanBarcode');
    return result;
  }
/*
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
*/

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
  }  @override
  Future<String> printPdfFromUrl(String url) async {
    final result = await methodChannel.invokeMethod<String>('printPdfFromUrl', {
      "pdfUrl": "https://www.example.com/invoice.pdf"
    },);
    return url ;
  }

  @override
  Future<String> printHtmlContent(String htmlContent) async{
    final result = await methodChannel.invokeMethod<String>('printHtmlContent',{
      'htmlContent':htmlContent
    });
    return result ?? '';
  } @override
  Future<String> printPdfFromPathOrAsset(String  path,bool isAsset) async{
   final result= await MethodChannel('zcs_pos_flutter').invokeMethod("printPdfFromPathOrAsset", {
      "pdfPath": "path",

      "isAsset": isAsset                   // ✅ Tell native it’s an asset
    });
    return result ?? '';
  }
}
