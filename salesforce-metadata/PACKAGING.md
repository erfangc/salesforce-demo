# Packaging Guide - Creating an Installation URL for Your Salesforce Components

This guide shows you step-by-step how to package your Case Echo Button (LWC + Apex) and create an installation URL you can share with others.

## What You're Packaging

Just the Salesforce UI components:
- ✅ Lightning Web Component (`caseEchoButton`)
- ✅ Apex class (`ChuckNorrisService`)
- ✅ Apex test class (`ChuckNorrisServiceTest`)
- ✅ Remote Site Setting (`ChuckNorrisAPI`)

**NOT packaging:**
- ❌ Your Java Spring Boot backend
- ❌ Your Connected App (that's for your backend)
- ❌ Your `.env.local` credentials

## Prerequisites

### 1. Install Salesforce CLI

```bash
# Install Salesforce CLI
npm install -g @salesforce/cli

# Verify installation
sf --version
```

**Download:** https://developer.salesforce.com/tools/salesforcecli

### 2. Two Salesforce Orgs

You'll need two separate Salesforce Developer Edition orgs:

1. **Packaging Org** (Dev Hub enabled)
   - Where you create and manage the package
   - Sign up: https://developer.salesforce.com/signup

2. **Test Org** (to test installation)
   - Where you'll test installing the package
   - Sign up: https://developer.salesforce.com/signup

**Why two orgs?** You can't install a package in the same org where it was created.

## Step-by-Step Process

### Step 1: Set Up Your Packaging Org (Dev Hub)

#### 1.1 Enable My Domain (If Not Already Done)

1. Log into your packaging org
2. Go to **Setup** → Search for "My Domain"
3. If not enabled:
   - Register a domain name (e.g., `yourname-dev-ed`)
   - Wait for domain to activate (can take 3-5 minutes)
   - Click **"Log in"** then **"Deploy to Users"**

**Why?** My Domain is required before you can enable Dev Hub or register a namespace.

#### 1.2 Enable Dev Hub

1. In Setup, search for **"Dev Hub"**
2. Click **"Enable Dev Hub"**
3. Toggle **"Enable Dev Hub"** to ON
4. Toggle **"Enable Unlocked Packages and Second-Generation Managed Packages"** to ON

**Result:** Your org can now create and manage packages.

#### 1.3 Register Your Namespace

1. In Setup, search for **"Package Manager"**
2. In the "Developer Settings" panel, click **"Edit"**
3. Under "Namespace Prefix":
   - Enter a unique prefix (e.g., `mycompany`, `johndoe`, etc.)
   - 1-15 alphanumeric characters, no spaces
   - Must be globally unique across ALL Salesforce orgs
4. Click **"Check Availability"**
5. If available, click **"Review My Selections"** then **"Save"**

**IMPORTANT:** You can only set this ONCE and it can never be changed. Choose carefully!

**What happens?** All your packaged components will be prefixed:
- `ChuckNorrisService` becomes `yournamespace__ChuckNorrisService`
- `caseEchoButton` becomes `yournamespace__caseEchoButton`

#### 1.4 Link Namespace to Dev Hub

1. After registering namespace, it should automatically link to Dev Hub
2. Verify: Setup → "Package Manager" should show "Linked to Dev Hub: Yes"

If not linked:
1. Go to Setup → "Package Manager"
2. Click "Link Namespace" and follow prompts

### Step 2: Authenticate Salesforce CLI to Your Orgs

#### 2.1 Authenticate to Packaging Org (Dev Hub)

```bash
# Navigate to your salesforce-metadata directory
cd /Users/erfangchen/IdeaProjects/salesforce-demo/salesforce-metadata

# Authenticate to packaging org
sf org login web --alias packaging-org --set-default-dev-hub

# This will open a browser - log in with your packaging org credentials
# After login, you can close the browser
```

**Verify:**
```bash
sf org display --target-org packaging-org
```

You should see org details including your namespace.

#### 2.2 Authenticate to Test Org (Optional - can do later)

```bash
# Authenticate to your test org
sf org login web --alias test-org

# This opens browser - log in with your TEST org credentials
```

### Step 3: Update Configuration Files

#### 3.1 Update Namespace in sfdx-project.json

Edit `sfdx-project.json` and replace the empty namespace with yours:

```json
{
  "packageDirectories": [
    {
      "path": "force-app",
      "default": true,
      "package": "CaseEchoPackage",
      "versionName": "Winter 2025",
      "versionNumber": "1.0.0.NEXT",
      "versionDescription": "Initial release: Case Echo Button with Chuck Norris API integration"
    }
  ],
  "name": "salesforce-demo-ui",
  "namespace": "yournamespace",  // <-- CHANGE THIS to your registered namespace
  "sfdcLoginUrl": "https://login.salesforce.com",
  "sourceApiVersion": "59.0",
  "packageAliases": {}
}
```

Save the file.

### Step 4: Create the Package

#### 4.1 Create Package Definition

```bash
# From the salesforce-metadata directory
sf package create \
  --name "CaseEchoPackage" \
  --description "Case Echo button with Chuck Norris integration" \
  --package-type Managed \
  --path force-app \
  --target-dev-hub packaging-org
```

**Expected Output:**
```
Successfully created a package. 0Ho... is the package ID.
```

**What just happened?**
- Salesforce created a package definition
- You got a Package ID (starts with `0Ho`)
- This ID is saved in `sfdx-project.json` under `packageAliases`

**Check your sfdx-project.json:**
```json
{
  ...
  "packageAliases": {
    "CaseEchoPackage": "0HoXXXXXXXXXXXXXXX"
  }
}
```

### Step 5: Create Package Version (This Takes Time!)

This step creates an installable version of your package:

```bash
sf package version create \
  --package "CaseEchoPackage" \
  --installation-key-bypass \
  --wait 20 \
  --target-dev-hub packaging-org
```

**What happens:**
1. Salesforce creates a temporary scratch org
2. Deploys your code to scratch org
3. Runs all your Apex tests
4. Verifies 75% code coverage
5. Creates package version
6. Deletes scratch org

**This takes 5-15 minutes!** The `--wait 20` means wait up to 20 minutes.

**Expected Output:**
```
Waiting for package version creation to complete....
Successfully created package version: 04tXXXXXXXXXXXXXXX
Package Version Id: 05iXXXXXXXXXXXXXXX
Branch:
Tag:
Installation URL: https://login.salesforce.com/packaging/installPackage.apexp?p0=04tXXXXXXXXXXXXXXX
```

**Save the Installation URL!** This is what you'll share with others.

**If you get errors:**
- **Code coverage too low:** Your tests must cover 75%+ of your Apex code
- **Deployment errors:** Fix any compilation errors in your code
- **Test failures:** Fix failing tests

**Check package version creation status:**
```bash
# If it's still running in background
sf package version create report --target-dev-hub packaging-org
```

### Step 6: Promote the Package Version (Make it Official)

The package version created above is initially in "beta" status. To make it production-ready:

```bash
sf package version promote \
  --package 04tXXXXXXXXXXXXXXX \
  --target-dev-hub packaging-org
```

Replace `04tXXXXXXXXXXXXXXX` with your actual package version ID from Step 5.

**Result:** Package version is now promoted and ready for production use!

### Step 7: Get Your Installation URL

Your installation URL format is:

```
Production/Developer Edition orgs:
https://login.salesforce.com/packaging/installPackage.apexp?p0=04tXXXXXXXXXXXXXXX

Sandbox orgs:
https://test.salesforce.com/packaging/installPackage.apexp?p0=04tXXXXXXXXXXXXXXX
```

Replace `04tXXXXXXXXXXXXXXX` with your package version ID (starts with `04t`).

## Testing Installation in Another Org

### Option A: Install via CLI

```bash
# Make sure you're authenticated to your test org
sf package install \
  --package 04tXXXXXXXXXXXXXXX \
  --target-org test-org \
  --wait 10 \
  --publish-wait 10
```

### Option B: Install via UI

1. Open your installation URL in a browser
2. Log in to your TEST org (NOT the packaging org!)
3. Choose installation option:
   - **"Install for Admins Only"** (recommended for testing)
   - **"Install for All Users"** (for production)
4. Check **"Grant access to these third-party websites"** (for Remote Site Settings)
5. Click **"Install"**
6. Wait for installation (2-5 minutes)

### Verify Installation

1. In your test org, go to **Setup** → **Installed Packages**
2. You should see **"CaseEchoPackage"**
3. Check **Setup** → **Remote Site Settings** → `yournamespace__ChuckNorrisAPI` should exist

### Add Component to Case Page

1. Navigate to **Setup** → **Object Manager** → **Case**
2. Click **Lightning Record Pages**
3. Edit your Case page (or create new)
4. Find `yournamespace:caseEchoButton` in the component list
5. Drag it onto the page
6. **Save** and **Activate**

### Test the Component

1. Go to **Cases** tab
2. Open any Case record
3. You should see the "Case Echo Button" component
4. Click **"Click Me"**
5. Should display toast with Case Number + Chuck Norris joke!

## Common Issues & Troubleshooting

### "No Default Dev Hub"

**Error:** `No default environment found`

**Solution:**
```bash
sf config set target-dev-hub=packaging-org
```

### "Namespace Already in Use"

**Error:** When trying to register namespace

**Solution:** Namespaces are globally unique. Try a different name.

### "Coverage Too Low" (Less than 75%)

**Error:** During package version creation

**Solution:** Your `ChuckNorrisServiceTest.cls` needs to cover more code. Run tests locally:

```bash
# Deploy to scratch org for testing
sf org create scratch --definition-file config/project-scratch-def.json --alias test-scratch

sf project deploy start --target-org test-scratch

# Run tests
sf apex run test --test-level RunLocalTests --target-org test-scratch --result-format human
```

Check coverage percentage in results.

### "Remote host not allowed" Error

**Error:** When clicking button in installed package

**Solution:**
1. Go to **Setup** → **Security** → **Remote Site Settings**
2. Find `yournamespace__ChuckNorrisAPI`
3. Make sure it's **Active**
4. If missing, manually add: `https://api.chucknorris.io`

### "Component Not Found"

**Error:** Can't find component in Lightning App Builder

**Solution:**
- Refresh the page
- Make sure you're editing a **Case** record page
- Check namespace is correct: `yournamespace:caseEchoButton`

### Package Version Creation Stuck

**Error:** `sf package version create` running for more than 20 minutes

**Solution:**
```bash
# Check status
sf package version create report --target-dev-hub packaging-org

# If failed, check errors in output
# Fix issues and try again
```

## Upgrading Your Package

When you make changes and want to release a new version:

### 1. Update Version Number

Edit `sfdx-project.json`:

```json
{
  "packageDirectories": [
    {
      "path": "force-app",
      "default": true,
      "package": "CaseEchoPackage",
      "versionName": "Summer 2025",  // <-- Updated
      "versionNumber": "1.1.0.NEXT",  // <-- Incremented
      "versionDescription": "Added feature X and fixed bug Y"
    }
  ],
  ...
}
```

### 2. Create New Version

```bash
sf package version create \
  --package "CaseEchoPackage" \
  --installation-key-bypass \
  --wait 20 \
  --target-dev-hub packaging-org
```

### 3. Promote New Version

```bash
sf package version promote --package <new-04t-id> --target-dev-hub packaging-org
```

### 4. Share New Installation URL

You'll get a new `04t...` ID. Share the updated installation URL with users.

**Subscribers can upgrade** by installing the new version using the new URL.

## What Subscribers Need to Know

Share these instructions with anyone installing your package:

### Installation Instructions

```markdown
# Installing Case Echo Package

1. Click the installation URL:
   https://login.salesforce.com/packaging/installPackage.apexp?p0=04tXXXXXXXXXXXXXXX

2. Log in to your Salesforce org

3. Choose: "Install for All Users"

4. Check: "Grant access to these third-party websites"

5. Click "Install" and wait 2-5 minutes

6. After installation:
   - Setup → Object Manager → Case → Lightning Record Pages
   - Edit your Case page
   - Add the "caseEchoButton" component
   - Save and activate

7. Test:
   - Open any Case record
   - Click the "Click Me" button
   - You should see a toast with case number and Chuck Norris joke!
```

## Quick Reference Commands

```bash
# Authenticate to packaging org
sf org login web --alias packaging-org --set-default-dev-hub

# Create package (one time only)
sf package create --name "CaseEchoPackage" --package-type Managed --path force-app

# Create package version
sf package version create --package "CaseEchoPackage" --installation-key-bypass --wait 20

# Promote version
sf package version promote --package 04t...

# List all package versions
sf package version list --package "CaseEchoPackage" --target-dev-hub packaging-org

# Install in another org
sf package install --package 04t... --target-org test-org --wait 10
```

## Resources

- [Salesforce CLI Setup Guide](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/)
- [Second-Generation Managed Packages](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_dev2gp.htm)
- [Package Version Creation](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_dev2gp_create_pkg_ver.htm)

## Need Help?

If you get stuck:
1. Check the "Common Issues & Troubleshooting" section above
2. Run commands with `--help` flag for more options
3. Check Salesforce CLI documentation
4. Verify your namespace is registered correctly

---

**Congratulations!** Once you complete these steps, you'll have a shareable installation URL for your Salesforce components!
