/*******************************************************************************
 * This file is part of Umbra.
 *
 *     Umbra is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Umbra is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Umbra.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2011 Vasile Jureschi <vasile.jureschi@gmail.com>.
 *     All rights reserved. This program and the accompanying materials
 *     are made available under the terms of the GNU Public License v3.0
 *     which accompanies this distribution, and is available at
 *
 *    http://www.gnu.org/licenses/gpl-3.0.html
 *
 *     Contributors:
 *        Vasile Jureschi <vasile.jureschi@gmail.com> - initial API and implementation
 *        Yen-Liang, Shen - Simplified Chinese and Traditional Chinese translations
 ******************************************************************************/


package org.unchiujar.umbra2.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import org.unchiujar.umbra2.R;

public class Help extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        final Button button = (Button) findViewById(R.id.feedback_btn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendEmail();
            }
        });
    }

    private void sendEmail() {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        String recipients[] = {
                getString(R.string.feedback_email)
        };

        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, recipients);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                getString(R.string.email_subject));
        emailIntent.setType("plain/text");

        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                getString(R.string.email_body));
        startActivity(Intent.createChooser(emailIntent,
                getString(R.string.email_chooser)));
    }

}
