/*
    This file is part of the Browser webview app.

    HHS Moodle WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HHS Moodle WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.popups;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.databases.DbAdapter_Bookmarks;
import de.baumann.browser.databases.DbAdapter_History;
import de.baumann.browser.databases.DbAdapter_ReadLater;
import de.baumann.browser.helper.helper_editText;
import de.baumann.browser.helper.helper_main;

public class Popup_history extends AppCompatActivity {

    private ListView listView = null;
    private EditText editText;
    private TextView urlBar;
    private DbAdapter_History db;
    private SimpleCursorAdapter adapter;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(Popup_history.this, R.color.colorThreeDark));

        setContentView(R.layout.activity_popup);
        helper_main.onStart(Popup_history.this);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.user_settings_search, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        editText = (EditText) findViewById(R.id.editText);
        editText.setVisibility(View.GONE);
        editText.setHint(R.string.app_search_hint);
        editText.clearFocus();
        urlBar = (TextView) findViewById(R.id.urlBar);
        setTitle();

        listView = (ListView)findViewById(R.id.list);

        //calling Notes_DbAdapter
        db = new DbAdapter_History(this);
        db.open();

        setHistoryList();
    }

    private void setHistoryList() {

        //display data
        final int layoutstyle=R.layout.list_item;
        int[] xml_id = new int[] {
                R.id.textView_title_notes,
                R.id.textView_des_notes,
                R.id.textView_create_notes
        };
        String[] column = new String[] {
                "history_title",
                "history_content",
                "history_creation"
        };
        final Cursor row = db.fetchAllData(this);
        adapter = new SimpleCursorAdapter(this, layoutstyle,row,column, xml_id, 0);

        //display data by filter
        final String note_search = sharedPref.getString("filter_historyBY", "history_title");
        sharedPref.edit().putString("filter_historyBY", "history_title").apply();
        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString());
            }
        });
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return db.fetchDataByFilter(constraint.toString(),note_search);
            }
        });

        listView.setAdapter(adapter);
        //onClick function
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {

                Cursor row = (Cursor) listView.getItemAtPosition(position);
                final String history_content = row.getString(row.getColumnIndexOrThrow("history_content"));
                sharedPref.edit().putString("openURL", history_content).apply();
                finish();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor row2 = (Cursor) listView.getItemAtPosition(position);
                final String _id = row2.getString(row2.getColumnIndexOrThrow("_id"));
                final String history_title = row2.getString(row2.getColumnIndexOrThrow("history_title"));
                final String history_content = row2.getString(row2.getColumnIndexOrThrow("history_content"));
                final String history_icon = row2.getString(row2.getColumnIndexOrThrow("history_icon"));
                final String history_attachment = row2.getString(row2.getColumnIndexOrThrow("history_attachment"));
                final String history_creation = row2.getString(row2.getColumnIndexOrThrow("history_creation"));

                final CharSequence[] options = {
                        getString(R.string.menu_share),
                        getString(R.string.menu_save),
                        getString(R.string.bookmark_edit_title),
                        getString(R.string.bookmark_remove_bookmark)};
                new AlertDialog.Builder(Popup_history.this)
                        .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        })
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(getString(R.string.bookmark_edit_title))) {
                                    sharedPref.edit().putString("edit_id", _id).apply();
                                    sharedPref.edit().putString("edit_content", history_content).apply();
                                    sharedPref.edit().putString("edit_icon", history_icon).apply();
                                    sharedPref.edit().putString("edit_attachment", history_attachment).apply();
                                    sharedPref.edit().putString("edit_creation", history_creation).apply();
                                    editText.setVisibility(View.VISIBLE);
                                    helper_editText.showKeyboard(Popup_history.this, editText, 2, history_title, getString(R.string.bookmark_edit_title));
                                }

                                if (options[item].equals(getString(R.string.bookmark_remove_bookmark))) {
                                    Snackbar snackbar = Snackbar
                                            .make(listView, R.string.bookmark_remove_confirmation, Snackbar.LENGTH_LONG)
                                            .setAction(R.string.toast_yes, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    db.delete(Integer.parseInt(_id));
                                                    setHistoryList();
                                                }
                                            });
                                    snackbar.show();
                                }

                                if (options[item].equals(getString(R.string.menu_share))) {
                                    final CharSequence[] options = {
                                            getString(R.string.menu_share_link),
                                            getString(R.string.menu_share_link_copy)};
                                    new AlertDialog.Builder(Popup_history.this)
                                            .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    dialog.cancel();
                                                }
                                            })
                                            .setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int item) {
                                                    if (options[item].equals(getString(R.string.menu_share_link))) {
                                                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                                        sharingIntent.setType("text/plain");
                                                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, history_title);
                                                        sharingIntent.putExtra(Intent.EXTRA_TEXT, history_content);
                                                        startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share_link))));
                                                    }
                                                    if (options[item].equals(getString(R.string.menu_share_link_copy))) {
                                                        ClipboardManager clipboard = (ClipboardManager) Popup_history.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("text", history_content));
                                                        Snackbar.make(listView, R.string.context_linkCopy_toast, Snackbar.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }).show();
                                }
                                if (options[item].equals(getString(R.string.menu_save))) {
                                    final CharSequence[] options = {
                                            getString(R.string.menu_save_bookmark),
                                            getString(R.string.menu_save_readLater),
                                            getString(R.string.menu_save_pass),
                                            getString(R.string.menu_createShortcut)};
                                    new AlertDialog.Builder(Popup_history.this)
                                            .setPositiveButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    dialog.cancel();
                                                }
                                            })
                                            .setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int item) {
                                                    if (options[item].equals(getString(R.string.menu_save_pass))) {
                                                        helper_editText.editText_savePass(Popup_history.this, listView, history_title, history_content);
                                                    }
                                                    if (options[item].equals(getString(R.string.menu_save_bookmark))) {

                                                        DbAdapter_Bookmarks db = new DbAdapter_Bookmarks(Popup_history.this);
                                                        db.open();

                                                        if(db.isExist(history_content)){
                                                            Snackbar.make(listView, getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                                                        }else{
                                                            db.insert(history_title, history_content, "", "", helper_main.createDate());
                                                            Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                                                        }
                                                    }
                                                    if (options[item].equals(getString(R.string.menu_save_readLater))) {
                                                        DbAdapter_ReadLater db = new DbAdapter_ReadLater(Popup_history.this);
                                                        db.open();
                                                        if(db.isExist(history_content)){
                                                            Snackbar.make(listView, getString(R.string.toast_newTitle), Snackbar.LENGTH_LONG).show();
                                                        }else{
                                                            db.insert(history_title, history_content, "", "", helper_main.createDate());
                                                            Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_LONG).show();
                                                        }
                                                    }
                                                    if (options[item].equals(getString(R.string.menu_createShortcut))) {
                                                        Intent i = new Intent();
                                                        i.setAction(Intent.ACTION_VIEW);
                                                        i.setClassName(Popup_history.this, "de.baumann.browser.Browser_left");
                                                        i.setData(Uri.parse(history_content));

                                                        Intent shortcut = new Intent();
                                                        shortcut.putExtra("android.intent.extra.shortcut.INTENT", i);
                                                        shortcut.putExtra("android.intent.extra.shortcut.NAME", "THE NAME OF SHORTCUT TO BE SHOWN");
                                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, history_title);
                                                        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(Popup_history.this.getApplicationContext(), R.mipmap.ic_launcher));
                                                        shortcut.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                                        Popup_history.this.sendBroadcast(shortcut);
                                                        Snackbar.make(listView, R.string.menu_createShortcut_success, Snackbar.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }).show();
                                }

                            }
                        }).show();
                return true;
            }
        });

        listView.post(new Runnable(){
            public void run() {
                listView.setSelection(listView.getCount() - 1);
            }});
    }

    private void setTitle () {
        if (sharedPref.getString("sortDBH", "title").equals("title")) {
            urlBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.sort_title));
        } else {
            urlBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.sort_date));
        }
    }

    @Override
    public void onBackPressed() {
        sharedPref.edit().putInt("keyboard", 0).apply();
        helper_main.isClosed(Popup_history.this);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Popup_history.this);
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isOpened(Popup_history.this);
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        helper_main.isClosed(Popup_history.this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (sharedPref.getInt("keyboard", 0) == 0) {
            // normal
            menu.findItem(R.id.action_cancel).setVisible(false);
            menu.findItem(R.id.action_save_bookmark).setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 1) {
            // filter
            menu.findItem(R.id.action_sort).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_save_bookmark).setVisible(false);
        } else if (sharedPref.getInt("keyboard", 0) == 2) {
            // save
            menu.findItem(R.id.action_filter).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_sort).setVisible(false);
        }

        return true; // this is important to call so that new menu is shown
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_popup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case R.id.filter_title:
                sharedPref.edit().putString("filter_historyBY", "history_title").apply();
                setHistoryList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(Popup_history.this, editText, 1, "", getString(R.string.action_filter_title));
                return true;
            case R.id.filter_url:
                sharedPref.edit().putString("filter_historyBY", "history_content").apply();
                setHistoryList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(Popup_history.this, editText, 1, "", getString(R.string.action_filter_url));
                return true;

            case R.id.filter_today:
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                final String search = dateFormat.format(cal.getTime());
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setText(search);
                urlBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.filter_today));
                return true;
            case R.id.filter_yesterday:
                DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal2 = Calendar.getInstance();
                cal2.add(Calendar.DATE, -1);
                final String search2 = dateFormat2.format(cal2.getTime());
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setText(search2);
                urlBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.filter_yesterday));
                return true;
            case R.id.filter_before:
                DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal3 = Calendar.getInstance();
                cal3.add(Calendar.DATE, -2);
                final String search3 = dateFormat3.format(cal3.getTime());
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setText(search3);
                urlBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.filter_before));
                return true;
            case R.id.filter_month:
                DateFormat dateFormat4 = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                Calendar cal4 = Calendar.getInstance();
                final String search4 = dateFormat4.format(cal4.getTime());
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setText(search4);
                urlBar.setText(getString(R.string.app_title_history) + " | " + getString(R.string.filter_month));
                return true;
            case R.id.filter_own:
                sharedPref.edit().putString("filter_historyBY", "history_creation").apply();
                setHistoryList();
                editText.setVisibility(View.VISIBLE);
                helper_editText.showKeyboard(Popup_history.this, editText, 1, "", getString(R.string.action_filter_create));
                return true;
            case R.id.filter_clear:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(Popup_history.this, editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setHistoryList();
                return true;

            case R.id.sort_title:
                sharedPref.edit().putString("sortDBH", "title").apply();
                setHistoryList();
                setTitle();
                return true;
            case R.id.sort_creation:
                sharedPref.edit().putString("sortDBH", "create").apply();
                setHistoryList();
                setTitle();
                return true;

            case R.id.action_cancel:
                editText.setVisibility(View.GONE);
                setTitle();
                helper_editText.hideKeyboard(Popup_history.this, editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setHistoryList();
                return true;

            case R.id.action_delete:
                Snackbar snackbar = Snackbar
                        .make(listView, R.string.toast_list, Snackbar.LENGTH_LONG)
                        .setAction(R.string.toast_yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Popup_history.this.deleteDatabase("history_DB_v01.db");
                                recreate();
                            }
                        });
                snackbar.show();
                return true;

            case R.id.action_save_bookmark:

                String edit_id = sharedPref.getString("edit_id", "");
                String edit_content = sharedPref.getString("edit_content", "");
                String edit_icon = sharedPref.getString("edit_icon", "");
                String edit_attachment = sharedPref.getString("edit_attachment", "");
                String edit_creation = sharedPref.getString("edit_creation", "");

                String inputTag = editText.getText().toString().trim();
                db.update(Integer.parseInt(edit_id), inputTag, edit_content, edit_icon, edit_attachment, edit_creation);
                helper_editText.hideKeyboard(Popup_history.this, editText, 0, getString(R.string.app_title_history), getString(R.string.app_search_hint));
                setHistoryList();

                Snackbar.make(listView, R.string.bookmark_added, Snackbar.LENGTH_SHORT).show();

                editText.setVisibility(View.GONE);
                setTitle();

                sharedPref.edit().putString("edit_id", "").apply();
                sharedPref.edit().putString("edit_content", "").apply();
                sharedPref.edit().putString("edit_icon", "").apply();
                sharedPref.edit().putString("edit_attachment", "").apply();
                sharedPref.edit().putString("edit_creation", "").apply();

                return true;

            case android.R.id.home:
                sharedPref.edit().putInt("keyboard", 0).apply();
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}