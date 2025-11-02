import { LightningElement, api, wire, track } from 'lwc';
import { getRecord, getFieldValue } from 'lightning/uiRecordApi';
import { ShowToastEvent } from 'lightning/platformShowToastEvent';
import CASE_NUMBER_FIELD from '@salesforce/schema/Case.CaseNumber';
import getRandomJoke from '@salesforce/apex/ChuckNorrisService.getRandomJoke';

const fields = [CASE_NUMBER_FIELD];

export default class CaseEchoButton extends LightningElement {
    @api recordId;
    @track isLoading = false;
    caseRecord;

    @wire(getRecord, { recordId: '$recordId', fields })
    wiredCase({ error, data }) {
        if (data) {
            this.caseRecord = data;
        } else if (error) {
            console.error('Error loading case:', error);
            this.showToast('Error', 'Unable to load case details', 'error');
        }
    }

    handleClick() {
        if (this.caseRecord) {
            const caseNumber = getFieldValue(this.caseRecord, CASE_NUMBER_FIELD);

            // Set loading state
            this.isLoading = true;

            // Call Chuck Norris API via Apex
            getRandomJoke()
                .then(result => {
                    // Success - show Case Number and Chuck Norris joke
                    const joke = result.value;
                    this.showToast(
                        'Case Info & Chuck Norris Fact',
                        `Case #${caseNumber} - ${joke}`,
                        'success'
                    );
                    this.isLoading = false;
                })
                .catch(error => {
                    // Error - show Case Number only
                    this.isLoading = false;
                    const errorMessage = error.body?.message || error.message || 'Unknown error';
                    console.error('Error fetching joke:', error);

                    // Still show Case Number even if API fails
                    this.showToast(
                        'Case Number',
                        `Case #${caseNumber} (Chuck Norris API unavailable: ${errorMessage})`,
                        'warning'
                    );
                });
        } else {
            this.showToast('Info', 'Case number not available yet', 'info');
        }
    }

    showToast(title, message, variant) {
        const event = new ShowToastEvent({
            title: title,
            message: message,
            variant: variant,
            mode: 'sticky' // Keep toast visible longer for joke readability
        });
        this.dispatchEvent(event);
    }
}
