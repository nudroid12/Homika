import fs from "node:fs/promises";
import path from "node:path";

const REQUIRED_SECRETS = [
  "LICENSE_SIGNING_PRIVATE_KEY",
  "CLOUD_MASTER_KEY",
  "HOMIKA_ADMIN_SECRET",
  "HOMIKA_ADMIN_TELEGRAM_BOT_TOKEN",
  "HOMIKA_ADMIN_TELEGRAM_CHAT_ID",
  "BREVO_API_KEY",
  "HOMIKA_PURCHASE_PIN_PEPPER",
];

const root = process.cwd();
const sourcePath = path.join(root, "wrangler.jsonc");
const generatedDir = path.join(root, ".homika-wrangler");
const generatedConfigPath = path.join(generatedDir, "wrangler.deploy.jsonc");
const redirectDir = path.join(root, ".wrangler", "deploy");
const redirectPath = path.join(redirectDir, "config.json");

function stripJsonComments(input) {
  let out = "";
  let inString = false;
  let escaped = false;
  let lineComment = false;
  let blockComment = false;

  for (let i = 0; i < input.length; i += 1) {
    const ch = input[i];
    const next = input[i + 1];

    if (lineComment) {
      if (ch === "\n") {
        lineComment = false;
        out += ch;
      }
      continue;
    }

    if (blockComment) {
      if (ch === "*" && next === "/") {
        blockComment = false;
        i += 1;
      } else if (ch === "\n") {
        out += ch;
      }
      continue;
    }

    if (inString) {
      out += ch;
      if (escaped) {
        escaped = false;
      } else if (ch === "\\") {
        escaped = true;
      } else if (ch === '"') {
        inString = false;
      }
      continue;
    }

    if (ch === '"') {
      inString = true;
      out += ch;
      continue;
    }

    if (ch === "/" && next === "/") {
      lineComment = true;
      i += 1;
      continue;
    }

    if (ch === "/" && next === "*") {
      blockComment = true;
      i += 1;
      continue;
    }

    out += ch;
  }

  return out;
}

function removeTrailingCommas(input) {
  let out = "";
  let inString = false;
  let escaped = false;

  for (let i = 0; i < input.length; i += 1) {
    const ch = input[i];

    if (inString) {
      out += ch;
      if (escaped) {
        escaped = false;
      } else if (ch === "\\") {
        escaped = true;
      } else if (ch === '"') {
        inString = false;
      }
      continue;
    }

    if (ch === '"') {
      inString = true;
      out += ch;
      continue;
    }

    if (ch === ",") {
      let j = i + 1;
      while (j < input.length && /\s/.test(input[j])) j += 1;
      if (input[j] === "}" || input[j] === "]") {
        continue;
      }
    }

    out += ch;
  }

  return out;
}

async function main() {
  const raw = await fs.readFile(sourcePath, "utf8");
  const parsed = JSON.parse(removeTrailingCommas(stripJsonComments(raw)));

  parsed.keep_vars = true;

  // The generated deploy config lives one directory below the backend root.
  // Wrangler resolves relative entry-point paths from the config file location,
  // so preserve the original target by rebasing `main` into .homika-wrangler/.
  if (typeof parsed.main === "string" && !path.isAbsolute(parsed.main)) {
    const absoluteMain = path.resolve(root, parsed.main);
    parsed.main = path.relative(generatedDir, absoluteMain).split(path.sep).join("/");
  }

  const existingRequired = Array.isArray(parsed?.secrets?.required)
    ? parsed.secrets.required.filter((value) => typeof value === "string")
    : [];

  parsed.secrets = {
    ...(parsed.secrets && typeof parsed.secrets === "object" ? parsed.secrets : {}),
    required: [...new Set([...existingRequired, ...REQUIRED_SECRETS])],
  };

  await fs.mkdir(generatedDir, { recursive: true });
  await fs.writeFile(
    generatedConfigPath,
    `${JSON.stringify(parsed, null, 2)}\n`,
    "utf8",
  );

  await fs.mkdir(redirectDir, { recursive: true });
  await fs.writeFile(
    redirectPath,
    `${JSON.stringify({ configPath: "../../.homika-wrangler/wrangler.deploy.jsonc" }, null, 2)}\n`,
    "utf8",
  );

  console.log("Homika Worker deploy config hardened.");
  console.log("- Dashboard vars preserved: keep_vars=true");
  console.log(`- Required secrets validated on deploy: ${REQUIRED_SECRETS.length}`);
  console.log(`- Worker entry point rebased for generated config: ${parsed.main}`);
  console.log("- Existing D1/R2 bindings copied from wrangler.jsonc without replacement.");
}

main().catch((error) => {
  console.error("Failed to prepare hardened Worker config:");
  console.error(error);
  process.exit(1);
});
