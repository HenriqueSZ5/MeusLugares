package br.edu.aula.meuslugares;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LugarFormActivity extends AppCompatActivity {

    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_PRECISAO = "precisao";
    public static final String EXTRA_APROXIMADA = "aproximada";

    // A Activity pode ser recriada enquanto a câmera está aberta
    private static final String ESTADO_CAPTURA = "arquivoCaptura";
    private static final String ESTADO_FOTO = "caminhoFoto";
    private static final String ESTADO_ENDERECO_BUSCADO = "enderecoBuscado";

    private double latitude;
    private double longitude;
    private Double precisao;

    private EditText edtNome;
    private EditText edtEndereco;
    private ProgressBar progressoEndereco;
    private TextView txtStatusEndereco;
    private Button btnBuscarEndereco;
    private ImageView imgFoto;
    private DBHelper dbHelper;

    private File arquivoCaptura; // o que a câmera está gravando agora
    private String caminhoFoto;  // foto já aceita
    private boolean salvo = false;
    private boolean enderecoBuscado = false; // evita repetir o GET ao girar a tela

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handlerTela = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> pedirCameraLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida) {
                    abrirCamera();
                } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Permissoes.mostrarDialogoConfiguracoes(this,
                            "Para tirar a foto do lugar, ative a permissão da câmera nas Configurações do app.");
                } else {
                    Toast.makeText(this, "Sem a permissão não dá para tirar foto", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> tirarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    sucesso -> tratarResultadoFoto(Boolean.TRUE.equals(sucesso)));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_lugar_form);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        dbHelper = new DBHelper(this);

        latitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
        longitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);
        if (getIntent().hasExtra(EXTRA_PRECISAO)) {
            precisao = getIntent().getDoubleExtra(EXTRA_PRECISAO, 0);
        }
        boolean aproximada = getIntent().getBooleanExtra(EXTRA_APROXIMADA, false);

        TextView txtCoordenadas = findViewById(R.id.txtCoordenadas);
        TextView txtAviso = findViewById(R.id.txtAviso);
        edtNome = findViewById(R.id.edtNome);
        edtEndereco = findViewById(R.id.edtEndereco);
        progressoEndereco = findViewById(R.id.progressoEndereco);
        txtStatusEndereco = findViewById(R.id.txtStatusEndereco);
        btnBuscarEndereco = findViewById(R.id.btnBuscarEndereco);
        imgFoto = findViewById(R.id.imgFoto);
        Button btnFoto = findViewById(R.id.btnFoto);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        String coordenadas = String.format(Locale.getDefault(), "Lat: %.6f\nLng: %.6f", latitude, longitude);
        if (precisao != null) {
            coordenadas += String.format(Locale.getDefault(), "\nPrecisão: ± %.0f m", precisao);
        }
        txtCoordenadas.setText(coordenadas);
        txtAviso.setVisibility(aproximada ? View.VISIBLE : View.GONE);

        if (savedInstanceState != null) {
            String captura = savedInstanceState.getString(ESTADO_CAPTURA);
            if (captura != null) arquivoCaptura = new File(captura);
            caminhoFoto = savedInstanceState.getString(ESTADO_FOTO);
            enderecoBuscado = savedInstanceState.getBoolean(ESTADO_ENDERECO_BUSCADO);
        }
        mostrarFoto();

        btnBuscarEndereco.setOnClickListener(v -> buscarEndereco());
        if (!enderecoBuscado) buscarEndereco();

        btnFoto.setOnClickListener(v -> verificarPermissaoCamera());
        btnSalvar.setOnClickListener(v -> salvar());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (arquivoCaptura != null) outState.putString(ESTADO_CAPTURA, arquivoCaptura.getAbsolutePath());
        outState.putString(ESTADO_FOTO, caminhoFoto);
        outState.putBoolean(ESTADO_ENDERECO_BUSCADO, enderecoBuscado);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        // Voltou sem salvar: a foto ficaria órfã na pasta
        if (isFinishing() && !salvo) apagar(caminhoFoto);
    }

    private void buscarEndereco() {
        mostrarBuscandoEndereco(true);

        executor.execute(() -> {
            String endereco = null;
            String erro = null;
            try {
                endereco = NominatimService.buscarEndereco(latitude, longitude);
            } catch (NominatimService.EnderecoException e) {
                erro = e.getMessage();
            } catch (IOException e) {
                erro = "Falha de conexão. Verifique a internet.";
            } catch (JSONException e) {
                erro = "Resposta inválida do servidor de endereços.";
            }

            String resultado = endereco; // a lambda precisa de variável final
            String mensagemErro = erro;
            handlerTela.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                mostrarBuscandoEndereco(false);
                if (resultado != null) {
                    enderecoBuscado = true;
                    // Não sobrescreve o que o usuário digitou durante a busca
                    if (edtEndereco.getText().toString().trim().isEmpty()) {
                        edtEndereco.setText(resultado);
                    }
                } else {
                    txtStatusEndereco.setText(mensagemErro);
                    txtStatusEndereco.setVisibility(View.VISIBLE);
                    btnBuscarEndereco.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void mostrarBuscandoEndereco(boolean buscando) {
        progressoEndereco.setVisibility(buscando ? View.VISIBLE : View.GONE);
        if (buscando) {
            edtEndereco.setHint("Buscando endereço...");
            txtStatusEndereco.setVisibility(View.GONE);
            btnBuscarEndereco.setVisibility(View.GONE);
        } else {
            edtEndereco.setHint(R.string.dica_endereco);
        }
    }

    private boolean temPermissaoCamera() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void verificarPermissaoCamera() {
        if (temPermissaoCamera()) {
            abrirCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Permissoes.mostrarExplicacao(this, "Precisamos da câmera",
                    "A câmera é usada apenas para tirar a foto do lugar que você está salvando.",
                    () -> pedirCameraLauncher.launch(Manifest.permission.CAMERA));
        } else {
            pedirCameraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamera() {
        try {
            arquivoCaptura = criarArquivoFoto();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", arquivoCaptura);
            tirarFotoLauncher.launch(uri);
        } catch (ActivityNotFoundException e) {
            arquivoCaptura = null;
            Toast.makeText(this, "Nenhum app de câmera encontrado", Toast.LENGTH_LONG).show();
        }
    }

    // Pasta do próprio app: dispensa permissão de armazenamento
    private File criarArquivoFoto() {
        String carimbo = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File pasta = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(pasta, "LUGAR_" + carimbo + ".jpg");
    }

    private void tratarResultadoFoto(boolean sucesso) {
        if (arquivoCaptura == null) return;

        if (sucesso && arquivoCaptura.exists() && arquivoCaptura.length() > 0) {
            apagar(caminhoFoto); // a anterior, se houver
            caminhoFoto = arquivoCaptura.getAbsolutePath();
            mostrarFoto();
        } else {
            apagar(arquivoCaptura.getAbsolutePath()); // cancelou, ficou vazio
            Toast.makeText(this, "Foto cancelada", Toast.LENGTH_SHORT).show();
        }
        arquivoCaptura = null;
    }

    private void mostrarFoto() {
        if (caminhoFoto != null) {
            imgFoto.setImageBitmap(Imagens.carregarReduzida(caminhoFoto, 800));
            imgFoto.setVisibility(View.VISIBLE);
        } else {
            imgFoto.setVisibility(View.GONE);
        }
    }

    private void apagar(String caminho) {
        if (caminho == null) return;
        File arquivo = new File(caminho);
        if (arquivo.exists()) arquivo.delete();
    }

    private void salvar() {
        String nome = edtNome.getText().toString().trim();
        if (nome.isEmpty()) {
            edtNome.setError("Digite o nome do lugar");
            return;
        }

        String data = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String endereco = edtEndereco.getText().toString().trim();
        long id = dbHelper.inserir(new Lugar(nome, latitude, longitude, precisao,
                endereco.isEmpty() ? null : endereco, caminhoFoto, data));

        if (id == -1) {
            Toast.makeText(this, "Erro ao salvar o lugar", Toast.LENGTH_SHORT).show();
            return;
        }
        salvo = true;
        Toast.makeText(this, "Lugar salvo!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
