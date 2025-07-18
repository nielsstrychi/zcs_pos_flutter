import 'package:flutter/material.dart';
import 'package:zcs_pos_flutter/zcs_pos_flutter.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(title: 'ZCS POS Demo', home: ZcsHomePage());
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
        _status = initialized ? 'SDK Initialized' : 'SDK Failed to Initialize';
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
      });
    }
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ZCS POS Flutter Demo')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Status: $_status'),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () async {
                bool success = await _zcsPosFlutter.printReceipt(
                  'Test Merchant',
                  '₹100.00',
                  ['Item 1 - ₹50.00', 'Item 2 - ₹30.00', 'Tax - ₹20.00'],
                );
                _showSnack(success ? 'Print successful' : 'Print failed');
              },
              child: const Text('Print Receipt'),
            ),
            ElevatedButton(
              onPressed: () async {
                bool success = await _zcsPosFlutter.showAmountOnDisplay(
                  '₹100.00',
                );
                _showSnack(success ? 'Display updated' : 'Display failed');
              },
              child: const Text('Show Amount on Display'),
            ),
            ElevatedButton(
              onPressed: () async {
                Map<String, dynamic>? cardData = await _zcsPosFlutter
                    .readNfcCard();
                _showSnack(
                  cardData != null
                      ? 'Card read: ${cardData.toString()}'
                      : 'Card read failed',
                );
              },
              child: const Text('Read NFC Card'),
            ),
            ElevatedButton(
              onPressed: () async {
                bool success = await _zcsPosFlutter.openCashDrawer();
                _showSnack(
                  success ? 'Cash drawer opened' : 'Cash drawer failed',
                );
              },
              child: const Text('Open Cash Drawer'),
            ),
          ],
        ),
      ),
    );
  }
}
