package com.example.shoppinglist


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController


class PartnerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_partner, container, false)

        val listView: ListView = view.findViewById(R.id.listView)
        val users = listOf("John", "Emma", "Alex", "Paul", "Sophia")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, users)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            findNavController().navigate(R.id.action_partnerFragment_to_shoppingListFragment)
        }

        return view
    }
}
