$ProjectPath = "C:\universalcrew-1.20.1-v3-source"
$CheckIntervalSeconds = 10

Set-Location $ProjectPath

Write-Host "========================================"
Write-Host " UniversalCrew - Auto GitHub Push"
Write-Host "========================================"
Write-Host "Klasor: $ProjectPath"
Write-Host "Kontrol araligi: $CheckIntervalSeconds saniye"
Write-Host ""

while ($true) {
    try {
        Set-Location $ProjectPath

        # Git'teki degisiklikleri kontrol et
        $changes = git status --porcelain 2>$null

        if ($changes) {
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Degisiklik algilandi."

            # Degisiklikleri ekle
            git add .

            # Commit
            $commitMessage = "Auto update $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
            git commit -m $commitMessage

            if ($LASTEXITCODE -eq 0) {
                # GitHub'a gonder
                git push

                if ($LASTEXITCODE -eq 0) {
                    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] GitHub guncellendi."
                }
                else {
                    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] PUSH BASARISIZ."
                }
            }
            else {
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] COMMIT BASARISIZ."
            }

            Write-Host ""
        }
    }
    catch {
        Write-Host "Hata: $($_.Exception.Message)"
    }

    Start-Sleep -Seconds $CheckIntervalSeconds
}