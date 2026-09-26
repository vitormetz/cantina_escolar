$ErrorActionPreference = "Stop"

$env:CANTINA_DB_URL = "jdbc:mysql://192.168.20.5:3306/cantina_escolar?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo"
$env:CANTINA_DB_USER = "root"

if ([string]::IsNullOrWhiteSpace($env:CANTINA_DB_PASSWORD)) {
    $segura = Read-Host "Senha do MySQL" -AsSecureString
    $ponteiro = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    try {
        $env:CANTINA_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ponteiro)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ponteiro)
    }
}

Write-Host "Compilando o projeto..." -ForegroundColor Cyan
mvn clean compile
if ($LASTEXITCODE -ne 0) { throw "A compilação falhou." }

Write-Host "Abrindo a Cantina Escolar..." -ForegroundColor Green
Write-Host "Na primeira abertura, o sistema concluirá automaticamente a migração do banco."
mvn exec:java "-Dexec.mainClass=TelaPrincipal"
