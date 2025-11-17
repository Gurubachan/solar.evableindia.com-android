package com.solar.ev.ui.common

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.solar.ev.R

class LoadingDialogFragment : DialogFragment() {

    private var messageTextView: TextView? = null

    companion object {
        const val TAG = "LoadingDialogFragment"
        internal const val ARG_MESSAGE = "message"

        fun newInstance(message: String): LoadingDialogFragment {
            val fragment = LoadingDialogFragment()
            val args = Bundle()
            args.putString(ARG_MESSAGE, message)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        val linearLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(48, 48, 48, 48) 
            gravity = android.view.Gravity.CENTER
        }

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleLarge).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isIndeterminate = true
        }

        messageTextView = TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = arguments?.getString(ARG_MESSAGE) ?: getString(R.string.processing_dialog_message)
            setPadding(0, 16, 0, 0) 
            textSize = 16f
            // Ensure the TextView has an ID if you need to find it by ID later, though direct reference is better here.
            // id = View.generateViewId() 
        }

        linearLayout.addView(progressBar)
        linearLayout.addView(messageTextView)

        return linearLayout
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            isCancelable = false
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    fun updateMessage(newMessage: String) {
        // Update the TextView if it's available
        messageTextView?.text = newMessage
        // Also update the arguments bundle in case the dialog is recreated (e.g., on rotation)
        // though for a simple loading dialog, this might be overkill if it's always dismissed and recreated.
        arguments?.putString(ARG_MESSAGE, newMessage)
    }

    override fun onDestroyView() {
        messageTextView = null // Clean up the reference
        super.onDestroyView()
    }
}
