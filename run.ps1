# Codeon launcher (Windows PowerShell)
# Asks for the MySQL password in the terminal, keeps it only in this session, then starts the site.
#   Run:  powershell -ExecutionPolicy Bypass -File .\run.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$secure = Read-Host 'Enter MySQL password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password

# Use the Maven next to the project folder if it exists, otherwise Maven from PATH
$bundled = Join-Path $PSScriptRoot '..\..\apache-maven-3.9.9\bin\mvn.cmd'
$mvn = if (Test-Path $bundled) { $bundled } else { 'mvn' }

try {
    & $mvn 'org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.14:run' | ForEach-Object {
        $_
        if ($_ -match 'Started ServerConnector') {
            Write-Host "`n>>> WEBSITE READY: http://localhost:8080/ <<<`n" -ForegroundColor Green
            Start-Process 'http://localhost:8080/'
        }
    }
}
finally {
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue   # runs on Ctrl+C too
}
