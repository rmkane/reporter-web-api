{{- /* CSV report for make scan. Every field is quoted, and embedded quotes are doubled. */ -}}
{{- define "field" }}"{{ . | replace "\"" "\"\"" }}"{{ end -}}
Target,Package,Installed,Fixed,Severity,Vulnerability,Title,URL
{{ range . }}{{ $target := .Target }}{{ range .Vulnerabilities -}}
{{ template "field" $target }},{{ template "field" .PkgName }},{{ template "field" .InstalledVersion }},{{ template "field" .FixedVersion }},{{ template "field" .Severity }},{{ template "field" .VulnerabilityID }},{{ template "field" .Title }},{{ template "field" .PrimaryURL }}
{{ end }}{{ end -}}
