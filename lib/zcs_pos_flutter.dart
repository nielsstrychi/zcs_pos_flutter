import 'dart:typed_data';

import 'zcs_pos_flutter_platform_interface.dart';

class ZcsPosFlutter {
  Future<bool> initializeSdk() {
    return ZcsPosFlutterPlatform.instance.initializeSdk();
  }



  Future<bool> showAmountOnDisplay(String amount) {
    return ZcsPosFlutterPlatform.instance.showAmountOnDisplay(amount);
  }

  Future<Map<String, dynamic>?> readNfcCard() {
    return ZcsPosFlutterPlatform.instance.readNfcCard();
  }

  Future<bool> openCashDrawer() {
    return ZcsPosFlutterPlatform.instance.openCashDrawer();
  }

  Future<String> printHtmlContent(String htmlContent) {
    return ZcsPosFlutterPlatform.instance.printHtmlContent(htmlContent);
  }

  Future<String> printPdfFromPathOrAsset(String  path,bool isAsset) {
    return ZcsPosFlutterPlatform.instance.printPdfFromPathOrAsset(path,isAsset);
  } Future<String> printPdfFromUrl(String  url) {
    return ZcsPosFlutterPlatform.instance.printPdfFromUrl(url,);
  }
  Future<bool> printReceipt({required String merchantName, required String billerName, required List<Map<String, dynamic>> items, required String netAmount, required String cashPaid}) async{
    // TODO: implement printReceipt
    final bool result = await ZcsPosFlutterPlatform.instance.printReceipt(merchantName: merchantName, billerName: billerName, items: items, netAmount: netAmount, cashPaid: cashPaid);

    return result;
  }  Future<bool> printBigText({required String text, }) async{
    // TODO: implement printReceipt
    final bool result = await ZcsPosFlutterPlatform.instance.printBigText(text:text);

    return result;
  } Future<bool> printBarcode({required String text, }) async{
    // TODO: implement printReceipt
    final bool result = await ZcsPosFlutterPlatform.instance.printBarcode(text:text);

    return result;
  }
}
