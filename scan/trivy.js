window.TRIVY_FAIL_ON = "HIGH,CRITICAL";
window.TRIVY_REPORT = {
  "SchemaVersion": 2,
  "CreatedAt": "2099-01-15T09:30:00Z",
  "ArtifactName": "example/sample-app:1.0.0",
  "ArtifactType": "container_image",
  "Results": [
    {
      "Target": "example/sample-app:1.0.0 (ubuntu 26.04)",
      "Class": "os-pkgs",
      "Type": "ubuntu",
      "Vulnerabilities": [
        {
          "VulnerabilityID": "CVE-2099-3001",
          "PkgName": "example-libc",
          "InstalledVersion": "2.43-1",
          "FixedVersion": "2.43-2",
          "Status": "fixed",
          "Severity": "CRITICAL",
          "Title": "Sample: Heap overflow in locale handling",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-3001"
        },
        {
          "VulnerabilityID": "CVE-2099-45012",
          "PkgName": "example-openssl",
          "InstalledVersion": "3.5.0-1",
          "FixedVersion": "3.5.0-2",
          "Status": "fixed",
          "Severity": "HIGH",
          "Title": "Sample: Timing side channel in RSA decryption",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-45012"
        },
        {
          "VulnerabilityID": "CVE-2099-8",
          "PkgName": "example-zlib",
          "InstalledVersion": "1.3-1",
          "Status": "affected",
          "Severity": "MEDIUM",
          "Title": "Sample: Out-of-bounds read on truncated input",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-8"
        },
        {
          "VulnerabilityID": "CVE-2099-910",
          "PkgName": "example-tar",
          "InstalledVersion": "1.35-1",
          "FixedVersion": "1.35-2",
          "Status": "fixed",
          "Severity": "LOW",
          "Title": "Sample: Path traversal through crafted symlinks",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-910"
        },
        {
          "VulnerabilityID": "CVE-2099-77001",
          "PkgName": "example-coreutils",
          "InstalledVersion": "9.5-1",
          "Status": "affected",
          "Severity": "LOW",
          "Title": "Sample: Race condition in temporary file creation",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-77001"
        },
        {
          "VulnerabilityID": "CVE-2099-55555",
          "PkgName": "example-bash",
          "InstalledVersion": "5.2-1",
          "Status": "affected",
          "Severity": "UNKNOWN",
          "Title": "Sample: Unspecified issue pending analysis"
        }
      ]
    },
    {
      "Target": "Java",
      "Class": "lang-pkgs",
      "Type": "jar",
      "Vulnerabilities": [
        {
          "VulnerabilityID": "CVE-2099-12001",
          "PkgName": "org.example:sample-web",
          "InstalledVersion": "4.2.0",
          "FixedVersion": "4.2.1, 4.3.0",
          "Status": "fixed",
          "Severity": "CRITICAL",
          "Title": "Sample: Request smuggling through malformed chunked encoding",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-12001"
        },
        {
          "VulnerabilityID": "CVE-2099-102473",
          "PkgName": "org.example:sample-json",
          "InstalledVersion": "1.9.0",
          "FixedVersion": "1.10.0",
          "Status": "fixed",
          "Severity": "HIGH",
          "Title": "Sample: Denial of service through deeply nested input",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-102473"
        },
        {
          "VulnerabilityID": "CVE-2099-65182",
          "PkgName": "org.example:sample-json",
          "InstalledVersion": "1.9.0",
          "FixedVersion": "1.9.4, 1.10.0",
          "Status": "fixed",
          "Severity": "HIGH",
          "Title": "Sample: Unbounded memory growth while buffering strings",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-65182"
        },
        {
          "VulnerabilityID": "CVE-2099-9999",
          "PkgName": "org.example:sample-auth",
          "InstalledVersion": "2.1.5",
          "FixedVersion": "2.1.6",
          "Status": "fixed",
          "Severity": "HIGH",
          "Title": "Sample: Authentication bypass through case-insensitive role match",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-9999"
        },
        {
          "VulnerabilityID": "CVE-2099-1001",
          "PkgName": "org.example:sample-parser",
          "InstalledVersion": "1.4.2",
          "FixedVersion": "1.4.3",
          "Status": "fixed",
          "Severity": "MEDIUM",
          "Title": "Sample: Unbounded recursion when parsing nested arrays",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-1001"
        },
        {
          "VulnerabilityID": "CVE-2099-20417",
          "PkgName": "org.example:sample-cache",
          "InstalledVersion": "2.0.0",
          "FixedVersion": "2.0.1, 2.1.0",
          "Status": "fixed",
          "Severity": "MEDIUM",
          "Title": "Sample: Cache key collision leaks entries across tenants",
          "PrimaryURL": "https://example.com/advisories/CVE-2099-20417"
        },
        {
          "VulnerabilityID": "GHSA-2099-samp-le01",
          "PkgName": "org.example:sample-xml",
          "InstalledVersion": "3.1.0",
          "Status": "affected",
          "Severity": "LOW",
          "Title": "Sample: External entity lookup enabled by default",
          "PrimaryURL": "https://example.com/advisories/GHSA-2099-samp-le01"
        },
        {
          "VulnerabilityID": "CVE-2099-100233",
          "PkgName": "org.example:sample-logging",
          "InstalledVersion": "0.9.8",
          "Status": "affected",
          "Severity": "UNKNOWN",
          "Title": "Sample: Log injection through unescaped newlines"
        }
      ]
    }
  ]
};
