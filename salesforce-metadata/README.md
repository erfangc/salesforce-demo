# Salesforce Case Echo Button - Lightning Web Component

This directory contains a Lightning Web Component (LWC) that displays a button on Case record pages. When clicked, it retrieves the Case Number and calls the Chuck Norris API to display a random Chuck Norris fact in a toast notification.

## Component Overview

**Component Name:** `caseEchoButton`

**Functionality:**
- Displays a "Click Me" button on Case Lightning Record pages
- When clicked, retrieves the current Case Number
- Calls the Chuck Norris API (via Apex) to fetch a random Chuck Norris fact
- Shows a toast notification combining the Case Number and Chuck Norris fact
- Displays loading spinner while API call is in progress
- Graceful error handling if API is unavailable

**Files:**
- **Lightning Web Component:**
  - `caseEchoButton.html` - Component template with button UI and loading spinner
  - `caseEchoButton.js` - JavaScript controller with API integration logic
  - `caseEchoButton.js-meta.xml` - Component metadata configuration
- **Apex Class:**
  - `ChuckNorrisService.cls` - Apex class for calling external Chuck Norris API
  - `ChuckNorrisService.cls-meta.xml` - Apex class metadata
- **Remote Site Settings:**
  - `ChuckNorrisAPI.remoteSite-meta.xml` - Authorizes https://api.chucknorris.io for callouts

## Prerequisites

1. **Salesforce CLI** - Install the latest version:
   ```bash
   npm install -g @salesforce/cli
   ```

2. **Salesforce Developer Edition Org** - You should already have one configured for this project

3. **Authentication** - Ensure you have access to your org with appropriate permissions

## Deployment Instructions

### Step 1: Authenticate with Your Salesforce Org

If you haven't already authenticated, run:

```bash
cd salesforce-metadata
sf org login web --alias myDevOrg
```

This will open a browser window where you can log in to your Developer Edition org.

To use your existing JWT-based authentication (matching your backend setup), you can instead run:

```bash
sf org login jwt --client-id 3MVG97L7PWbPq6Uz_RALb6O1RF5fn1wDxiF_15HtPXA0IArecr3xj2mw7KyKwGBnEcdq35h2pbazb7K0xvAQC \
  --jwt-key-file ../src/main/resources/sf-jwt-private.key \
  --username custodian.bot@example.dev \
  --alias myDevOrg \
  --set-default
```

### Step 2: Deploy the Component

From the `salesforce-metadata` directory:

```bash
sf project deploy start --source-dir force-app
```

Expected output:
```
Deploying v59.0 metadata to custodian.bot@example.dev...
Status: Succeeded
Component              Type                      Status
─────────────────────  ────────────────────────  ────────
ChuckNorrisAPI         RemoteSiteSetting         Created
ChuckNorrisService     ApexClass                 Created
caseEchoButton         LightningComponentBundle  Created/Updated
```

**Note:** Remote Site Settings and Apex classes will be deployed along with the LWC component.

### Step 3: Add Component to Case Lightning Page

1. Navigate to any Case record in your Salesforce org
2. Click the gear icon (Setup) in the top right
3. Select **Edit Page** from the dropdown
4. In the Lightning App Builder:
   - Find **caseEchoButton** in the Custom components list (left sidebar)
   - Drag and drop it onto any region of the page (typically the right sidebar)
5. Click **Save**
6. Click **Activate** (if this is the first time editing this page)
7. Assign the page to the appropriate org default or app defaults
8. Click **Save** again

### Step 4: Test the Component

1. Navigate to any Case record
2. You should see the "Case Echo Button" card with a "Click Me" button
3. Click the button
4. You'll see a loading spinner with "Fetching Chuck Norris fact..."
5. A success toast notification should appear displaying:
   - **Title:** "Case Info & Chuck Norris Fact"
   - **Message:** "Case #00001234 - [Random Chuck Norris fact]"
6. The toast will stay visible longer (sticky mode) so you can read the joke

**Example Toast Message:**
```
Case Info & Chuck Norris Fact
Case #00001234 - Chuck Norris can divide by zero.
```

## Troubleshooting

### API Callout Errors

**Issue:** Toast shows warning with "Chuck Norris API unavailable" message.

**Possible Causes:**
1. **Remote Site Settings not deployed or not active**
   - Verify in Setup → Security → Remote Site Settings
   - Ensure `ChuckNorrisAPI` is listed and Active
   - URL should be `https://api.chucknorris.io`

2. **Apex class compilation errors**
   - Check in Setup → Apex Classes
   - Look for `ChuckNorrisService` and verify it's Active
   - Check the browser console for JavaScript errors

3. **Network/firewall issues**
   - Salesforce may be blocking the external domain
   - Check if https://api.chucknorris.io is accessible from your network
   - Review Debug Logs in Setup → Debug Logs for callout details

**Solution - Manual Remote Site Configuration:**
If automatic deployment didn't work, manually configure:
1. Setup → Security → Remote Site Settings
2. Click "New Remote Site"
3. Remote Site Name: `ChuckNorrisAPI`
4. Remote Site URL: `https://api.chucknorris.io`
5. Disable Protocol Security: Unchecked
6. Active: Checked
7. Save

### Redeploying After Changes

If you make changes to the component and need to redeploy:

```bash
# From the salesforce-metadata directory
sf project deploy start --source-dir force-app
```

This will update all modified components. You don't need to remove the component from Lightning pages - it will update automatically.

**For quick iterations during development:**
```bash
# Deploy with validation only (no actual deployment)
sf project deploy start --source-dir force-app --dry-run

# Deploy and automatically run local tests
sf project deploy start --source-dir force-app --test-level RunLocalTests
```

### Component Not Appearing in Lightning App Builder

**Issue:** Can't find `caseEchoButton` in the custom components list.

**Solutions:**
- Ensure deployment was successful (check deployment status)
- Refresh the Lightning App Builder page
- Verify you're editing a Case Lightning Record Page (not Account, Contact, etc.)
- Check the component metadata file has correct target configuration

### "Case number not available yet" Message

**Issue:** Button shows info toast saying case number isn't available.

**Solutions:**
- Wait a moment and try again (component may still be loading)
- Refresh the page
- Check that the Case record actually exists and has a Case Number

### Deployment Errors

**Issue:** Deployment fails with error messages.

**Solutions:**
- Ensure you're authenticated to the correct org:
  ```bash
  sf org display --target-org myDevOrg
  ```
- Check that your user has appropriate permissions (System Administrator or equivalent)
- Verify all required files exist in the correct directory structure
- Check for syntax errors in the component files

### General Debugging

Enable debug mode for more information:

```bash
sf project deploy start --source-dir force-app --verbose
```

Check deployment status:

```bash
sf project deploy report
```

## Component Architecture

### JavaScript Controller (`caseEchoButton.js`)

- **@api recordId**: Automatically populated with the current Case record ID
- **@track isLoading**: Tracks loading state for UI feedback
- **@wire getRecord**: Retrieves the Case record with CaseNumber field
- **Import Apex**: Imports `getRandomJoke` method from `ChuckNorrisService` Apex class
- **handleClick()**: Orchestrates API call and toast display:
  1. Gets Case Number from record
  2. Sets loading state to true
  3. Calls Apex method imperatively (not @wire, since it's an HTTP callout)
  4. Shows toast with Case Number + Chuck Norris fact on success
  5. Shows warning toast with Case Number only if API fails
- **showToast()**: Helper method with sticky mode for better readability

### HTML Template (`caseEchoButton.html`)

- Uses Lightning Design System (SLDS) styling
- `lightning-card` for container
- `lightning-button` with brand variant, disabled during loading
- `lightning-spinner` conditionally rendered when `isLoading` is true
- Loading message for user feedback

### Apex Class (`ChuckNorrisService.cls`)

- **getRandomJoke()**: Apex method annotated with `@AuraEnabled`
  - Creates HTTP request to https://api.chucknorris.io/jokes/random
  - Parses JSON response into `JokeResponse` wrapper class
  - Handles errors and throws `AuraHandledException` for LWC
  - 10-second timeout for HTTP callout
- **JokeResponse**: Inner wrapper class matching API response structure
  - Properties: `id`, `icon_url`, `url`, `value` (joke text), `categories`
  - All properties annotated with `@AuraEnabled` for LWC access

### Remote Site Settings (`ChuckNorrisAPI.remoteSite-meta.xml`)

- Authorizes Salesforce to make HTTP callouts to https://api.chucknorris.io
- Required for all external API integrations from Apex
- Must be deployed before Apex callouts will work

### Metadata Configuration (`caseEchoButton.js-meta.xml`)

- API Version: 59.0 (matching your backend)
- Exposed: true (makes it available in Lightning App Builder)
- Target: `lightning__RecordPage` (Lightning Record Pages)
- Object: Case (only available for Case objects)

### Integration Flow

```
User clicks button
    ↓
LWC handleClick() called
    ↓
LWC shows loading spinner
    ↓
LWC calls Apex via getRandomJoke()
    ↓
Apex makes HTTP GET to Chuck Norris API
    ↓
API returns JSON with joke
    ↓
Apex deserializes JSON to JokeResponse
    ↓
LWC receives JokeResponse
    ↓
LWC combines Case Number + joke
    ↓
LWC shows toast notification
    ↓
LWC hides loading spinner
```

## Future Enhancements

Potential improvements you could make:

1. **Backend Integration**: Call your Spring Boot backend API to log button clicks
2. **Additional Fields**: Display more Case information (Status, Priority, Subject)
3. **Styling Options**: Add configuration properties for button label, color, etc.
4. **Action Menu**: Convert to a Quick Action instead of page component
5. **Close Case**: Add functionality to close the case using your existing backend API

## Related Backend Code

This LWC component is standalone but could integrate with your existing Spring Boot backend:

- **Backend Endpoint**: `POST /cases/{caseId}/close` (see `CaseController.kt`)
- **CDC Monitoring**: Your backend already listens to Case changes via `CaseChangeEvent`

To integrate, you would need to:
1. Create a Named Credential in Salesforce pointing to your backend
2. Create an Apex class to make HTTP callouts
3. Update the LWC JavaScript to call the Apex method

## Directory Structure

```
salesforce-metadata/
├── .forceignore                           # Files to exclude from deployment
├── README.md                              # This file (deployment guide)
├── sfdx-project.json                      # SFDX project configuration
└── force-app/
    └── main/
        └── default/
            ├── classes/                   # Apex classes
            │   ├── ChuckNorrisService.cls            # Apex HTTP callout service
            │   └── ChuckNorrisService.cls-meta.xml   # Apex metadata
            ├── lwc/                       # Lightning Web Components
            │   └── caseEchoButton/
            │       ├── caseEchoButton.html           # Component template
            │       ├── caseEchoButton.js             # Component logic
            │       └── caseEchoButton.js-meta.xml    # Component metadata
            └── remoteSiteSettings/        # Remote Site Settings
                └── ChuckNorrisAPI.remoteSite-meta.xml # API authorization
```

## Resources

- [Lightning Web Components Developer Guide](https://developer.salesforce.com/docs/component-library/documentation/en/lwc)
- [Salesforce CLI Command Reference](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_reference.meta/sfdx_cli_reference/)
- [Lightning Design System](https://www.lightningdesignsystem.com/)
- [Toast Events Documentation](https://developer.salesforce.com/docs/component-library/documentation/en/lwc/lwc.use_toast)

## Support

For issues or questions:
1. Check the Troubleshooting section above
2. Review Salesforce developer documentation
3. Check your backend logs if integrating with Spring Boot backend
4. Verify your Developer Edition org permissions and setup
