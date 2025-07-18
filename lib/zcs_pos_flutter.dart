import 'zcs_pos_flutter_platform_interface.dart';

class ZcsPosFlutter {
  Future<bool> initializeSdk() {
    return ZcsPosFlutterPlatform.instance.initializeSdk();
  }

  Future<bool> printReceipt(
    String merchantName,
    String amount,
    List<String> items,
  ) {
    return ZcsPosFlutterPlatform.instance.printReceipt(
      merchantName,
      amount,
      items,
    );
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
}
