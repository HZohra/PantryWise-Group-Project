# Diagrams

Every diagram of the documentation is a PNG image, so the Markdown files do not depend on a renderer.

| Folder / file | Content |
|---|---|
| `img/` | The PNG files used by the documents. Named `<document>-<number>.png`, in order of appearance |
| `src/` | The Mermaid source of each PNG (`<document>-<number>.mmd`) |

## Regenerate

Edit the `.mmd` file, render it and commit both the source and the PNG.

```
# one Mermaid diagram (needs Node.js and a Chromium based browser)
npx -p @mermaid-js/mermaid-cli@11.4.2 mmdc -i src/02-design-01.mmd -o img/02-design-01.png -s 2 -w 1400 -b white -p puppeteer.json
```

`puppeteer.json` is `{ "executablePath": "<path to msedge.exe or chrome.exe>" }`, needed only when mermaid-cli has no browser of its own.