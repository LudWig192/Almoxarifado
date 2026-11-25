package com.example.almoxarifado;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class ScannerActivity extends AlmoxActivity {
    static final int CAMERA = 17;
    DecoratedBarcodeView scanner;
    TextInputLayout til;
    TextInputEditText code;
    boolean paused, flash, choose;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_scanner);
        toolbar(R.id.toolbar);
        scanner = findViewById(R.id.barcodeView);
        til = findViewById(R.id.tilCodigo);
        code = findViewById(R.id.etCodigo);
        choose = ESCOLHER_ITEM.equals(getIntent().getStringExtra(MODO));
        scanner.decodeContinuous(result -> {
            if (!paused) {
                paused = true;
                handle(result.getText());
            }
        });
        findViewById(R.id.btnBuscarCodigo).setOnClickListener(v -> manual());
        View torch = findViewById(R.id.btnLanterna);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            torch.setVisibility(View.GONE);
        torch.setOnClickListener(v -> {
            flash = !flash;
            try {
                if (flash) scanner.setTorchOn();
                else scanner.setTorchOff();
            } catch (Exception ignored) {
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA);
        else scanner.resume();
    }

    void manual() {
        String c = String.valueOf(code.getText()).trim();
        if (c.isEmpty()) {
            til.setError("Informe o código.");
            return;
        }
        if (!c.matches("\\d+")) {
            til.setError("Use apenas números.");
            return;
        }
        til.setError(null);
        paused = true;
        handle(c);
    }

    void handle(String c) {
        AppData.Item i = AppData.RepositoryProvider.ITENS.buscar(c);
        if (i != null) {
            if (choose) {
                setResult(RESULT_OK, new Intent().putExtra(CODIGO, c));
                finish();
            } else goCode(FichaItemActivity.class, c);
            return;
        }
        Snackbar.make(findViewById(android.R.id.content), "Nenhum item cadastrado com este código.", Snackbar.LENGTH_LONG).setAction(user != null && user.perfil == AppData.Perfil.ALMOXARIFE ? "Cadastrar" : null, v -> goCode(CadastroItemActivity.class, c)).addCallback(new Snackbar.Callback() {
            public void onDismissed(Snackbar s, int e) {
                paused = false;
                if (scanner != null && ContextCompat.checkSelfPermission(ScannerActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    scanner.resume();
            }
        }).show();
    }

    @Override
    public void onRequestPermissionsResult(int q, String[] p, int[] g) {
        super.onRequestPermissionsResult(q, p, g);
        if (q == CAMERA) {
            if (g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) scanner.resume();
            else {
                Toast.makeText(this, "Câmera negada. Você ainda pode digitar o código.", Toast.LENGTH_LONG).show();
                findViewById(R.id.barcodeView).setVisibility(View.GONE);
                findViewById(R.id.btnLanterna).setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanner != null && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            scanner.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scanner != null) scanner.pause();
    }
}