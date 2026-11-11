Add-Type -AssemblyName System.Drawing

$resDir = "c:\Users\Administrator\Desktop\BuildApps\MycoNet\android\app\src\main\res"
$sizes = @{
    "mipmap-mdpi"    = 48
    "mipmap-hdpi"    = 72
    "mipmap-xhdpi"   = 96
    "mipmap-xxhdpi"  = 144
    "mipmap-xxxhdpi" = 192
}

function Generate-Icon($size, $isRound, $outPath) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $rect = New-Object System.Drawing.Rectangle 0, 0, $size, $size
    $bgBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 11, 15, 23)) # Dark tactical slate

    if ($isRound) {
        $g.FillEllipse($bgBrush, 1, 1, $size - 2, $size - 2)
        $borderPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 16, 185, 129)), ([float]($size * 0.03))
        $g.DrawEllipse($borderPen, 1, 1, $size - 2, $size - 2)
    } else {
        # Rounded rectangle
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $radius = [float]($size * 0.22)
        $dia = $radius * 2
        $path.AddArc(0, 0, $dia, $dia, 180, 90)
        $path.AddArc($size - $dia - 1, 0, $dia, $dia, 270, 90)
        $path.AddArc($size - $dia - 1, $size - $dia - 1, $dia, $dia, 0, 90)
        $path.AddArc(0, $size - $dia - 1, $dia, $dia, 90, 90)
        $path.CloseFigure()
        $g.FillPath($bgBrush, $path)

        $borderPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 16, 185, 129)), ([float]($size * 0.03))
        $g.DrawPath($borderPen, $path)
        $path.Dispose()
    }

    # Center coordinates
    $cx = [float]($size / 2)
    $cy = [float]($size / 2)
    $r = [float]($size * 0.28)

    # Nodes in a mesh pattern
    $nodes = @(
        @{ X = $cx; Y = $cy; R = [float]($size * 0.09) } # Center
        @{ X = $cx; Y = $cy - $r; R = [float]($size * 0.06) } # Top
        @{ X = [float]($cx + $r * 0.866); Y = [float]($cy - $r * 0.5); R = [float]($size * 0.06) } # Top-right
        @{ X = [float]($cx + $r * 0.866); Y = [float]($cy + $r * 0.5); R = [float]($size * 0.06) } # Bottom-right
        @{ X = $cx; Y = $cy + $r; R = [float]($size * 0.06) } # Bottom
        @{ X = [float]($cx - $r * 0.866); Y = [float]($cy + $r * 0.5); R = [float]($size * 0.06) } # Bottom-left
        @{ X = [float]($cx - $r * 0.866); Y = [float]($cy - $r * 0.5); R = [float]($size * 0.06) } # Top-left
    )

    # Mesh links (connecting center to all outer nodes, and outer nodes in hexagon)
    $meshPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(180, 16, 185, 129)), ([float]($size * 0.025))
    $crossPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(100, 52, 211, 153)), ([float]($size * 0.015))

    for ($i = 1; $i -le 6; $i++) {
        $g.DrawLine($meshPen, $nodes[0].X, $nodes[0].Y, $nodes[$i].X, $nodes[$i].Y)
        $next = if ($i -eq 6) { 1 } else { $i + 1 }
        $g.DrawLine($meshPen, $nodes[$i].X, $nodes[$i].Y, $nodes[$next].X, $nodes[$next].Y)
    }
    # Cross connections
    $g.DrawLine($crossPen, $nodes[1].X, $nodes[1].Y, $nodes[4].X, $nodes[4].Y)
    $g.DrawLine($crossPen, $nodes[2].X, $nodes[2].Y, $nodes[5].X, $nodes[5].Y)
    $g.DrawLine($crossPen, $nodes[3].X, $nodes[3].Y, $nodes[6].X, $nodes[6].Y)

    # Draw outer nodes
    $nodeBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 52, 211, 153))
    $centerBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
    $glowPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 16, 185, 129)), ([float]($size * 0.03))

    for ($i = 1; $i -le 6; $i++) {
        $nx = $nodes[$i].X - $nodes[$i].R
        $ny = $nodes[$i].Y - $nodes[$i].R
        $nd = $nodes[$i].R * 2
        $g.FillEllipse($nodeBrush, $nx, $ny, $nd, $nd)
    }

    # Draw center hub node with glow
    $cnx = $nodes[0].X - $nodes[0].R
    $cny = $nodes[0].Y - $nodes[0].R
    $cnd = $nodes[0].R * 2
    $g.DrawEllipse($glowPen, $cnx - 2, $cny - 2, $cnd + 4, $cnd + 4)
    $g.FillEllipse($centerBrush, $cnx, $cny, $cnd, $cnd)

    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

foreach ($folder in $sizes.Keys) {
    $dim = $sizes[$folder]
    $dirPath = Join-Path $resDir $folder
    if (-not (Test-Path $dirPath)) { New-Item -ItemType Directory -Path $dirPath -Force }

    $launcherPath = Join-Path $dirPath "ic_launcher.png"
    $roundPath = Join-Path $dirPath "ic_launcher_round.png"

    Generate-Icon $dim $false $launcherPath
    Generate-Icon $dim $true $roundPath
    Write-Output "Generated $folder ($dim x $dim)"
}

# Also save a copy in brain artifacts for inspection
$artifactDir = "C:\Users\Administrator\.gemini\antigravity\brain\071ee0d5-790d-4dba-af4f-bb4303447308"
Generate-Icon 192 $false "$artifactDir\myconet_new_icon.png"
Write-Output "Artifact saved to $artifactDir\myconet_new_icon.png"
