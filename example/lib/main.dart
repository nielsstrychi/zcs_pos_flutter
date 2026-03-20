import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:zcs_pos_flutter/zcs_pos_flutter.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      title: 'ZCS POS Demo',
      home: ZcsHomePage(),
    );
  }
}

class ZcsHomePage extends StatefulWidget {
  const ZcsHomePage({super.key});

  @override
  State<ZcsHomePage> createState() => _ZcsHomePageState();
}

class _ZcsHomePageState extends State<ZcsHomePage> {
  final _zcsPosFlutter = ZcsPosFlutter();
  String _status = 'Not initialized';

  @override
  void initState() {
    super.initState();
    _initializeSDK();
  }

  Future<void> _initializeSDK() async {
    try {
      bool initialized = await _zcsPosFlutter.initializeSdk();
      setState(() {
        _status = initialized ? '✅ SDK Initialized' : '❌ SDK Failed to Initialize';
      });
      _showSnack(_status);
    } catch (e) {
      setState(() => _status = '❌ Error: $e');
      _showSnack("SDK initialization failed: $e");
    }
  }

  void _showSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), duration: const Duration(seconds: 2)),
    );
  }

  Future<void> _printReceipt() async {
    try {
      bool success = await _zcsPosFlutter.printReceipt(
      merchantName: 'Your Store Name',
        items: [
          {
            'name':"Very Long Product Name That Needs Wrapping Because It's Too Long",
            'qty':1,
            'price':25.50,
            'amount':25.50,
          }, {
            'name':"Short Item",
            'qty':1,
            'price':25.50,
            'amount':25.50,
          }
        ],
        billerName: 'Admin',
        netAmount: "100.00",
        cashPaid:"99.00",
      );
      _showSnack(success ? '✅ Receipt printed successfully' : '❌ Print failed');
    } catch (e) {
      _showSnack("Print failed: $e");
    }
  }

  Future<void> _printHtml() async {
    try {
      String result = await _zcsPosFlutter.printHtmlContent(
          """<html> ... your HTML ... </html>"""
      );
      _showSnack(result);
    } catch (e) {
      _showSnack("HTML Print failed: $e");
    }
  }


  Future<void> printPdfWithPdfRender(
      BuildContext context,
      String pdfPath,
      bool is80mm,
      ) async {
    final zcsPos = ZcsPosFlutter();

    try {
    zcsPos.printPdfFromPathOrAsset('assets/Invoice.pdf', true);
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("PDF print failed: $e")),
      );
    }
  }  Future<void> printPdfUsingUrl(
      BuildContext context,

      ) async {
    final zcsPos = ZcsPosFlutter();

    try {
    zcsPos.printPdfFromUrl('https://morth.nic.in/sites/default/files/dd12-13_0.pdf', );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("PDF print failed: $e")),
      );
    }
  }


  Future<void> _showAmountOnDisplay() async {
    try {
      bool success = await _zcsPosFlutter.showAmountOnDisplay('₹100.00');
      _showSnack(success ? '✅ Display updated' : '❌ Display failed');
    } catch (e) {
      _showSnack("Display failed: $e");
    }
  }

  Future<void> _readNfcCard() async {
    try {
      Map<String, dynamic>? cardData = await _zcsPosFlutter.readNfcCard();
      _showSnack(
        cardData != null
            ? '✅ Card read: ${cardData.toString()}'
            : '❌ Card read failed',
      );
    } catch (e) {
      _showSnack("NFC read failed: $e");
    }
  }

  Future<void> _openCashDrawer() async {
    try {
      bool success = await _zcsPosFlutter.openCashDrawer();
      _showSnack(success ? '✅ Cash drawer opened' : '❌ Cash drawer failed');
    } catch (e) {
      _showSnack("Cash drawer failed: $e");
    }
  }

  Future<void> _scanBarcode() async {
    try {
      _showSnack("Scanning... please wait up to 15s");
      String? result = await _zcsPosFlutter.scanBarcode();
      if (result != null) {
        _showSnack("✅ Scanned: $result");
      } else {
        _showSnack("❌ Scan timeout or failed");
      }
    } catch (e) {
      _showSnack("Scan failed: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ZCS POS Flutter Demo')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: ListView(
          children: [
            Text('Status: $_status'),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () async{
               await _zcsPosFlutter.printBigText(text: '260.00');
               // await _zcsPosFlutter.printBarcode(text: '260.00');
              },
              child: const Text('Print Receipt'),
            ),
            ElevatedButton(
              onPressed: _printHtml,
              child: const Text('Print HTML'),
            ),
            ElevatedButton(
              onPressed: ()=>printPdfWithPdfRender(context,'assets/Invoice.pdf',true),
              child: const Text('Print PDF'),
            ), ElevatedButton(
              onPressed: ()=>printPdfUsingUrl(context),
              child: const Text('Print PDF Using URL'),
            ),
            ElevatedButton(
              onPressed: _showAmountOnDisplay,
              child: const Text('Show Amount on Display'),
            ),
            ElevatedButton(
              onPressed: _readNfcCard,
              child: const Text('Read NFC Card'),
            ),
            ElevatedButton(
              onPressed: _openCashDrawer,
              child: const Text('Open Cash Drawer'),
            ),
            ElevatedButton(
              onPressed: _scanBarcode,
              child: const Text('Scan Barcode'),
            ),
          ],
        ),
      ),
    );
  }
}
