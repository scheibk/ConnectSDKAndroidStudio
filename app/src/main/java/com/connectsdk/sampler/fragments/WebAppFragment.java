//
//  Connect SDK Sample App by LG Electronics
//
//  To the extent possible under law, the person who associated CC0 with
//  this sample app has waived all copyright and related or neighboring rights
//  to the sample app.
//
//  You should have received a copy of the CC0 legalcode along with this
//  work. If not, see http://creativecommons.org/publicdomain/zero/1.0/.
//

package com.connectsdk.sampler.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.connectsdk.sampler.R;
import com.connectsdk.service.CastService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession;
import com.connectsdk.service.sessions.WebAppSession.LaunchListener;
import com.connectsdk.service.sessions.WebAppSessionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebAppFragment extends BaseFragment {
	public final static String TAG = "Connect SDK";
	public Button launchWebAppButton;
	public Button joinWebAppButton;
	public Button leaveWebAppButton;
	public Button closeWebAppButton;
	public Button sendMessageButton;
	public Button sendJSONButton;
	TextView responseMessageTextView;
    LaunchSession runningAppSession;
    
    WebAppSession mWebAppSession;

	public WebAppFragment(Context context) 
	{
		super(context);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(
				R.layout.fragment_webapp, container, false);
		
		launchWebAppButton = (Button) rootView.findViewById(R.id.launchWebAppButton);
		joinWebAppButton = (Button) rootView.findViewById(R.id.joinWebAppButton);
		leaveWebAppButton = (Button) rootView.findViewById(R.id.leaveWebAppButton);
		closeWebAppButton = (Button) rootView.findViewById(R.id.closeWebAppButton);
		sendMessageButton = (Button) rootView.findViewById(R.id.sendMessageButton);
		sendJSONButton = (Button) rootView.findViewById(R.id.sendJSONButton);
		responseMessageTextView = (TextView) rootView.findViewById(R.id.responseMessageTextView);
		
		buttons = new Button[]{
				launchWebAppButton, 
				joinWebAppButton, 
				leaveWebAppButton, 
				closeWebAppButton, 
				sendMessageButton, 
				sendJSONButton
		};
		
		return rootView;
	}

	@Override
	public void enableButtons() {
		super.enableButtons();
		
		if (getTv().hasCapability(WebAppLauncher.Launch)) {
			launchWebAppButton.setOnClickListener(launchWebApp);
		}
		else {
			disableButton(launchWebAppButton);
		}
		
		joinWebAppButton.setEnabled(getTv().hasCapability(WebAppLauncher.Launch));
		joinWebAppButton.setOnClickListener(joinWebApp);
		
		leaveWebAppButton.setEnabled(getTv().hasCapability(WebAppLauncher.Disconnect));
		leaveWebAppButton.setOnClickListener(leaveWebApp);
		
		if (getTv().hasCapability(WebAppLauncher.Close)) {
			closeWebAppButton.setOnClickListener(closeWebApp);
		}
		
		if (getTv().hasCapability(WebAppLauncher.Message_Send)) {
			sendMessageButton.setOnClickListener(sendMessage);
			sendJSONButton.setOnClickListener(sendJson);
		}
		
		responseMessageTextView.setText("");
		
		disableButton(closeWebAppButton);
		disableButton(leaveWebAppButton);
		disableButton(sendMessageButton);
		disableButton(sendJSONButton);
	}
	
	public View.OnClickListener launchWebApp = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String webAppId = "";
			if (getTv().getServiceByName(WebOSTVService.ID) != null)
				webAppId = "SampleWebApp";
			else if (getTv().getServiceByName(CastService.ID) != null)
				webAppId = "DDCEDE96";
			else
				return;

			launchWebAppButton.setEnabled(false);
			
			getTv().getWebAppLauncher().launchWebApp(webAppId, new LaunchListener() {
				
				@Override
				public void onError(ServiceCommandError error) {
					Log.e("LG", "Error connecting to web app | error = " + error);
					launchWebAppButton.setEnabled(true);
				}
				
				@Override
				public void onSuccess(WebAppSession webAppSession) {
					webAppSession.setWebAppSessionListener(webAppListener);
					
					if (getTv().hasAnyCapability(WebAppLauncher.Message_Send, WebAppLauncher.Message_Receive, WebAppLauncher.Message_Receive_JSON, WebAppLauncher.Message_Send_JSON))
						webAppSession.connect(connectionListener);
					else
						connectionListener.onSuccess(webAppSession.launchSession);
					
					mWebAppSession = webAppSession;
				}
			});
		}
	};
	
	public View.OnClickListener joinWebApp = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String webAppId = "";
			
			if (getTv().getServiceByName(WebOSTVService.ID) != null)
				webAppId = "SampleWebApp";
			else if (getTv().getServiceByName(CastService.ID) != null)
				webAppId = "DDCEDE96";
			else
				return;
			
			getTv().getWebAppLauncher().joinWebApp(webAppId, new LaunchListener() {
				
				@Override
				public void onError(ServiceCommandError error) {
					Log.d("LG", "Could not join");
				}
				
				@Override
				public void onSuccess(WebAppSession webAppSession) {
					if (getTv() == null)
						return;
					
					webAppSession.setWebAppSessionListener(webAppListener);
					mWebAppSession = webAppSession;
					
					sendMessageButton.setEnabled(true);
					launchWebAppButton.setEnabled(false);
					leaveWebAppButton.setEnabled(getTv().hasCapability(WebAppLauncher.Disconnect));
					if (getTv().hasCapabilities(WebAppLauncher.Message_Send_JSON)) sendJSONButton.setEnabled(true);
					if (getTv().hasCapabilities(WebAppLauncher.Close)) closeWebAppButton.setEnabled(true);
				}
			});
		}
	};
	
	public View.OnClickListener leaveWebApp = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (mWebAppSession != null) {
				mWebAppSession.setWebAppSessionListener(null);
				mWebAppSession.disconnectFromWebApp();
				mWebAppSession = null;
				
				launchWebAppButton.setEnabled(true);
				joinWebAppButton.setEnabled(getTv().hasCapability(WebAppLauncher.Join));
				sendMessageButton.setEnabled(false);
				sendJSONButton.setEnabled(false);
				leaveWebAppButton.setEnabled(false);
				closeWebAppButton.setEnabled(false);
			}
		}
	};
	
	public WebAppSessionListener webAppListener = new WebAppSessionListener() {
		
		@Override
		public void onReceiveMessage(WebAppSession webAppSession, Object message) {
			Log.d(TAG, "Message received from app | " + message);
			
			if (message.getClass() == String.class)
			{
				responseMessageTextView.append((String) message);
				responseMessageTextView.append("\n");
			} else if (message.getClass() == JSONObject.class)
			{
				responseMessageTextView.append(((JSONObject) message).toString());
				responseMessageTextView.append("\n");
			}
		}

		@Override
		public void onWebAppSessionDisconnect(WebAppSession webAppSession) {
			Log.d("LG", "Device was disconnected");
			
			if (webAppSession != mWebAppSession) {
				webAppSession.setWebAppSessionListener(null);
				return;
			}
			
			launchWebAppButton.setEnabled(true);
			if (getTv() != null) joinWebAppButton.setEnabled(getTv().hasCapability(WebAppLauncher.Join));
			sendMessageButton.setEnabled(false);
			sendJSONButton.setEnabled(false);
			leaveWebAppButton.setEnabled(false);
			closeWebAppButton.setEnabled(false);
			
			mWebAppSession.setWebAppSessionListener(null);
			mWebAppSession = null;
		}
	};
	
	public ResponseListener<Object> connectionListener = new ResponseListener<Object>() {
		
		@Override
		public void onSuccess(Object response) {
			if (getTv() == null)
				return;
			
			if (getTv().hasCapability(WebAppLauncher.Message_Send_JSON))
				sendJSONButton.setEnabled(true);
			
			if (getTv().hasCapability(WebAppLauncher.Message_Send))
				sendMessageButton.setEnabled(true);
			
			leaveWebAppButton.setEnabled(getTv().hasCapability(WebAppLauncher.Disconnect));
			
			closeWebAppButton.setEnabled(true);
			launchWebAppButton.setEnabled(false);
		}
		
		@Override
		public void onError(ServiceCommandError error) {
			sendJSONButton.setEnabled(false);
			sendMessageButton.setEnabled(false);
			closeWebAppButton.setEnabled(false);
			launchWebAppButton.setEnabled(true);
			
			if (mWebAppSession != null) {
				mWebAppSession.setWebAppSessionListener(null);
				mWebAppSession.close(null);
			}
		}
	};
	
	public View.OnClickListener sendMessage = new View.OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			String message = "This is an Android test message.";
			
			mWebAppSession.sendMessage(message, new ResponseListener<Object>() {
				
				@Override
				public void onSuccess(Object response) {
					Log.d(TAG, "Sent message : " + response);
				}
				
				@Override
				public void onError(ServiceCommandError error) {
					Log.e(TAG, "Error sending message : " + error);
				}
			});
		}
	};
	
	public View.OnClickListener sendJson = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			JSONObject message = null;
			try {
				message = new JSONObject() {{
					put("type", "message");
					put("contents", "This is a test message");
					put("params", new JSONObject() {{
						put("someParam1", "The content & format of this JSON block can be anything");
						put("someParam2", "The only limit ... is yourself");
					}});
					put("anArray", new JSONArray() {{
						put("Just");
						put("to");
						put("show");
						put("we");
						put("can");
						put("send");
						put("arrays!");
					}});
				}};
			} catch (JSONException e) {
				return;
			}

			mWebAppSession.sendMessage(message, new ResponseListener<Object>() {
				
				@Override
				public void onSuccess(Object response) {
					Log.d(TAG, "Sent message : " + response);
				}
				
				@Override
				public void onError(ServiceCommandError error) {
					Log.e(TAG, "Error sending message : " + error);
				}
			});
		}
	};
	
	public View.OnClickListener closeWebApp = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			responseMessageTextView.setText("");
			
			closeWebAppButton.setEnabled(false);
			sendMessageButton.setEnabled(false);
			sendJSONButton.setEnabled(false);
			leaveWebAppButton.setEnabled(false);
			
			mWebAppSession.setWebAppSessionListener(null);
			mWebAppSession.close(new ResponseListener<Object>() {
				
				@Override
				public void onSuccess(Object response) {
					launchWebAppButton.setEnabled(true);
				}
				
				@Override
				public void onError(ServiceCommandError error) {
					Log.e(TAG, "Error closing web app | error = " + error);
					
					launchWebAppButton.setEnabled(true);
				}
			});
		}
	};
	
	@Override
	public void disableButtons() {
		super.disableButtons();
		
		responseMessageTextView.setText("");
	}
	
	public void setRunningAppInfo(LaunchSession session) {
    	runningAppSession = session;
    }
}
