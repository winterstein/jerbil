import React, { Component } from 'react';
import { assert } from 'sjtest';
import Login from 'you-again';
import md5 from 'md5';
import { modifyHash } from 'wwutils';
// Plumbing
import JSend from '../base/data/JSend';
import DataStore from '../base/plumbing/DataStore';
import Roles from '../base/Roles';
import C from '../C';
// import ServerIO from '../plumbing/ServerIO';
// import ActionMan from '../plumbing/ActionMan';
// Widgets
import Messaging, {notifyUser} from '../base/plumbing/Messaging';
import MessageBar from '../base/components/MessageBar';
import NavBar from '../base/components/NavBar';
import Misc from '../base/components/Misc';

import CardAccordion, {Card} from '../base/components/CardAccordion';
import PropControl from '../base/components/PropControl';
import BS4 from '../base/components/BS4';

// Pages
import E404Page from '../base/components/E404Page';

class MainDiv extends Component {

	componentWillMount() {
		// redraw on change
		const updateReact = (mystate) => this.setState({});
		DataStore.addListener(updateReact);

		Login.app = C.app.service;
		// Set up login watcher here, at the highest level		
		Login.change(() => {
			// ?? should we store and check for "Login was attempted" to guard this??
			if (Login.isLoggedIn()) {
				// close the login dialog on success
				LoginWidget.hide();
			} else {
				// poke React via DataStore (e.g. for Login.error)
				DataStore.update({});
			}
			this.setState({});
		});

		// Are we logged in?
		Login.verify();
	}

	componentDidCatch(error, info) {
		// Display fallback UI
		this.setState({error, info, errorPath: DataStore.getValue('location', 'path')});
		console.error(error, info); 
		if (window.onerror) window.onerror("Caught error", null, null, null, error);
	}

	render() {
		return (
			<div className="container avoid-navbar">
				<MessageBar />
				<div className="page MyPage">
					<TextEditor />
					<Preview />
				</div>
			</div>
		);
	} // ./render()
} // ./MainDiv

const TextEditor = () => {
	const page = DataStore.getUrlValue("page");
	if ( ! page) {
		return <div>Set a page</div>;
	}
	const pvPage = DataStore.fetch(["data","pages",page,'text'], () => {
		return $.get('/pages/'+escape(page));
	});
	return <PropControl path={["data","pages",page]} prop='text' />;
};

const Preview = () => {
	return <iframe style={{width:'50%',height:'100%'}} src='https://doc.good-loop.com' />;
};

export default MainDiv;
