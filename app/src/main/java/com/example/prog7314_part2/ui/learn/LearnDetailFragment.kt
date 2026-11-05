package com.example.prog7314_part2.ui.learn

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.LearnGuideDto
import com.example.prog7314_part2.databinding.FragmentLearnDetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LearnDetailFragment : Fragment() {

    private var _binding: FragmentLearnDetailBinding? = null
    private val binding get() = _binding!!
    private var guideCall: Call<LearnGuideDto>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLearnDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnRetryDetail.setOnClickListener { loadGuide() }
        loadGuide()
    }

    private fun loadGuide() {
        val screen = _binding ?: return
        val guideId = arguments?.getString("guideId").orEmpty()
        if (guideId.isBlank()) {
            showError(screen)
            return
        }

        guideCall?.cancel()
        screen.detailLoading.isVisible = true
        screen.detailErrorGroup.isVisible = false
        screen.detailScroll.isVisible = false

        val call = ApiClient.service.getLearnGuide(guideId)
        guideCall = call

        call.enqueue(object : Callback<LearnGuideDto> {
            override fun onResponse(call: Call<LearnGuideDto>, response: Response<LearnGuideDto>) {
                if (_binding !== screen) return
                screen.detailLoading.isVisible = false

                val guide = response.body()
                if (!response.isSuccessful || guide == null) {
                    Log.w(TAG, "detail http=${response.code()} guideId=$guideId")
                    showError(screen)
                    return
                }
                Log.i(TAG, "detail loaded id=${guide.id} blob=${LearnSupport.isBlobUrl(guide.mediaUrl)}")
                bind(screen, guide)
            }

            override fun onFailure(call: Call<LearnGuideDto>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                screen.detailLoading.isVisible = false
                Log.w(TAG, "detail failed guideId=$guideId: ${error.message}")
                showError(screen)
            }
        })
    }

    private fun bind(screen: FragmentLearnDetailBinding, guide: LearnGuideDto) {
        screen.detailScroll.isVisible = true
        screen.detailErrorGroup.isVisible = false
        screen.guideTitle.text = guide.title
        screen.guideCategory.text = LearnSupport.categoryLabel(guide.category)
        screen.guideBody.text = guide.body

        if (LearnSupport.isImageUrl(guide.mediaUrl)) {
            screen.guideImage.isVisible = true
            screen.guideImage.contentDescription = guide.title
            Glide.with(this)
                .load(guide.mediaUrl)
                .centerCrop()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.w(TAG, "blob image failed url=${guide.mediaUrl}: ${e?.message}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.i(TAG, "blob image ready url=${guide.mediaUrl} source=$dataSource")
                        return false
                    }
                })
                .into(screen.guideImage)
        } else {
            screen.guideImage.isVisible = false
        }

        val videoUrl = guide.mediaUrl
        if (LearnSupport.isVideoUrl(videoUrl)) {
            screen.btnOpenVideo.isVisible = true
            screen.btnOpenVideo.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                } catch (error: Exception) {
                    Toast.makeText(requireContext(), "No app can open this video link.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            screen.btnOpenVideo.isVisible = false
        }
    }

    private fun showError(screen: FragmentLearnDetailBinding) {
        screen.detailScroll.isVisible = false
        screen.detailErrorGroup.isVisible = true
    }

    override fun onDestroyView() {
        guideCall?.cancel()
        guideCall = null
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "Learn"
    }
}
